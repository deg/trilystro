## Extension details

From https://chrome.google.com/webstore/developer/dashboard (logged in as david.goldfarb@vuagain.com)

```
Item ID: dikihecbidmmfpjjkofpcfcpdfefnilf
Public Key:
-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAiKwYUoo01NFnvJOE3MXB
eNOAzxjlJYq5JK7tCpirHR76yKNIbh1bw2OdiXODwovsVxx0ygVZm5j6qElHiCWE
BSWVsGtWlGiA48eRZV/NwSmOc5OrpEo5OqGDr38/onmDzWQcUmGzxD09Yq2wZ6oL
5FAcTjq48YnV+hyoO3PsIbchYnRbLDx56ZHCD6AB5HfrfjdNULNHViyJJeWBJ9Z+
iH+D2NQ0an1gWwkJ45r+oJIcFzTwLwTbUdMU9OZtLjZP23Lh+hnOzpNq5hUTmiya
u5zIiQsb+zw8WyhtOLHx711B04Jw2IQR0CWJszXuUdGcsPuCtP5bfNK5jPYN8Vu2
tQIDAQAB
-----END PUBLIC KEY-----
```

Client Id: `856783092972-5eptaefsf6lsqo6rvslvar22l94nq2cu.apps.googleusercontent.com`

Unpacked ID: `agodcgdobplkmjdejemipkdmbhehfbmp`
Release ID:  `njoghiokmjpjojgpppnpolimeffoopgf`

At some point, may need to add `"key"` to the manifests. See
https://developer.chrome.com/apps/manifest/key and what I did in the VuAgain prototype.


## Setting up Firebase

Old method, using Google auth workaround is at
<https://github.com/firebase/quickstart-js/tree/master/auth/chromextension>. We are not
using this.

Instead see
https://firebase.google.com/docs/auth/web/google-signin#authenticate_with_firebase_in_a_chrome_extension;
also referenced in https://github.com/firebase/quickstart-js/issues/112 in comment on 13Sep17.

