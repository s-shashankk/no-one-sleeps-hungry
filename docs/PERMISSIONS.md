# Endpoint permissions

There is one kind of signed-in person: the admin. Everything except the public forms and sign-in needs a signed-in admin, enforced twice: by URL rules in `SecurityConfig` and again in each service through `AccessPolicy`. `security/EndpointPermissionsTest` walks **every** mapped endpoint and fails if an anonymous caller gets anything other than 401, or if a new public endpoint appears. Add any new endpoint to this table.

| Endpoint (`/api/v1/...`) | Anonymous | Signed-in admin |
|---|---|---|
| `POST public/hunger-reports`, `POST public/surplus-food-reports` | yes (rate limited, honeypot, consent) – they only create *pending* requests | yes |
| `POST auth/login` (rate limited), `GET auth/csrf`, `POST auth/logout`, `GET health` | yes | yes |
| `GET auth/me` | no | yes |
| `vendors` (list, get, add, edit) | no | yes |
| `coworkers` (list, get, add, edit) | no | yes |
| `requests` (list, get, add, edit, complete, accept, reject, reopen) | no | yes |
| `dashboard` | no | yes |

Privacy: names, phone numbers and locations are returned only to signed-in admins. The public forms never echo anything back except a thank-you message.
