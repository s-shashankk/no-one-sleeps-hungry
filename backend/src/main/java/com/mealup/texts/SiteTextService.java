package com.mealup.texts;

import java.time.Duration;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mealup.access.AccessPolicy;
import com.mealup.common.exception.BadRequestException;
import com.mealup.common.validation.Text;

/** Reads (public) and changes (admin only) the editable wording of the website. */
@Service
public class SiteTextService {

    private static final Pattern KEY = Pattern.compile("[a-z0-9][a-z0-9._-]{0,79}");
    private static final int MAX_VALUE = 1500;
    private static final int MAX_ITEMS = 300;

    /** Visitors get a copy kept in memory for this long, so the database is not asked on every page load. */
    private static final Duration KEEP = Duration.ofMinutes(10);

    private final SiteTextRepository repo;
    private final AccessPolicy access;
    private volatile Map<String, String> cached;
    private volatile long cachedAtMillis;

    public SiteTextService(SiteTextRepository repo, AccessPolicy access) {
        this.repo = repo;
        this.access = access;
    }

    /** What visitors see: {@link #edited()} kept in memory for a few minutes. Saving a change clears it. */
    public Map<String, String> forVisitors() {
        Map<String, String> copy = cached;
        if (copy == null || System.currentTimeMillis() - cachedAtMillis > KEEP.toMillis()) {
            copy = Map.copyOf(edited());
            cached = copy;
            cachedAtMillis = System.currentTimeMillis();
        }
        return copy;
    }

    /** Only the wording the admin has changed: key to text. Anything missing uses the website's own default. */
    @Transactional(readOnly = true)
    public Map<String, String> edited() {
        Map<String, String> out = new TreeMap<>();
        repo.findAll().forEach(t -> out.put(t.getKey(), t.getValue()));
        return out;
    }

    /** A blank text removes the change, so the website goes back to its original wording. */
    @Transactional
    public Map<String, String> save(Map<String, String> values) {
        access.requireUser();
        if (values == null || values.isEmpty()) return edited();
        if (values.size() > MAX_ITEMS) throw new BadRequestException("Too many changes at once.");
        for (Map.Entry<String, String> e : values.entrySet()) {
            String key = e.getKey();
            if (key == null || !KEY.matcher(key).matches()) throw new BadRequestException("Unknown text name.");
            String text = Text.clean(e.getValue());
            if (text == null) {
                repo.findById(key).ifPresent(repo::delete);
                continue;
            }
            if (text.length() > MAX_VALUE) throw new BadRequestException("That text is too long (the limit is 1500 letters).");
            repo.findById(key).ifPresentOrElse(t -> t.change(text), () -> repo.save(new SiteText(key, text)));
        }
        repo.flush();
        cached = null;   // the next visitor reads the new wording
        return edited();
    }
}
