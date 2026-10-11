# CI setup: secrets, variables and options

What to configure for the two GitHub pipelines, and what each setting does:

- **App** ([tadamiorg/tadami](https://github.com/tadamiorg/tadami)): builds and tests every push and pull request to `develop`, and releases the app when you run the Release workflow, which computes the version from the commits.
- **Extensions** ([tadamiorg/tadami-extensions-source](https://github.com/tadamiorg/tadami-extensions-source)): builds the extensions you pick and publishes them to [tadamiorg/tadami-extensions](https://github.com/tadamiorg/tadami-extensions), the repo the app downloads extensions from.

The release process itself (how versions are computed, the release flow) is in [RELEASING.md](RELEASING.md).

## Checklist

| Repo | Name | Kind | Needed | What it's for |
| --- | --- | --- | --- | --- |
| tadami | `KEYSTORE_BASE64` | Secret | Yes, to release | App signing keystore |
| tadami | `KEY_STORE_PASSWORD` | Secret | Yes, to release | Its password |
| tadami | `KEY_PASSWORD` | Secret | Optional | Key password, if different from the keystore password |
| tadami | `ALIAS` | Secret | Yes, to release | Key alias |
| tadami | `RELEASE_CERT_SHA256` | Variable | Recommended | Refuses a release signed with the wrong key |
| tadami | `RELEASE_DRAFT` | Variable | Optional | `false` publishes releases right away; otherwise they're drafts |
| tadami-extensions-source | `EXTENSIONS_KEYSTORE_BASE64` | Secret | Yes | Extensions signing keystore |
| tadami-extensions-source | `EXTENSIONS_KEY_STORE_PASSWORD` | Secret | Yes | Its password (also the key password) |
| tadami-extensions-source | `EXTENSIONS_KEY_ALIAS` | Secret | Yes | Key alias |
| tadami-extensions-source | `EXTENSIONS_REPO_TOKEN` | Secret | Yes, to publish | Token allowed to push to tadami-extensions |

Secrets go in the repo's **Settings > Secrets and variables > Actions > Secrets**, variables in the **Variables** tab of the same page. Secrets are hidden once saved; variables stay readable.

---

## App repo (tadamiorg/tadami)

### Workflows

| Workflow | File | Runs on | Needs settings |
| --- | --- | --- | --- |
| CI | `.github/workflows/ci.yml` | Pushes and pull requests to `develop`, or Actions > CI > Run workflow | No |
| Release | `.github/workflows/release.yml` | Actions > Release > Run workflow, on `main` | Yes, see below |
| Dependabot | `.github/dependabot.yml` | Monthly | No: it opens pull requests on `develop` updating the workflow actions |

**CI** runs the unit tests, builds the debug APK and an unsigned release APK (R8 included), runs lint (informational: lint errors don't fail it), then the instrumented tests on an Android 14 emulator. It uploads the debug APK and the test and lint reports as artifacts. It never uses the signing secrets.

**Release**:
1. computes the version from the commits since the last `vX.Y.Z` tag (RELEASING.md, "Versions");
2. decodes the keystore, runs the unit tests and builds the signed APK with that version;
3. checks the APK's version, verifies the signature, and compares the certificate with `RELEASE_CERT_SHA256` when it's set;
4. pushes the tag `vX.Y.Z` and creates the GitHub release `Tadami vX.Y.Z`, **as a draft** unless `RELEASE_DRAFT` is `false`, with notes built from the commits, a SHA-256 checksum and `tadami-vX.Y.Z.apk` as its only asset;
5. keeps the R8 mapping as the workflow artifact `mapping-vX.Y.Z`.

### Run options

Actions > Release > Run workflow asks for one input:

| Input | What it does |
| --- | --- |
| `bump` | `auto` (default) picks the bump from the commits since the last tag: major for a breaking change, minor for a `feat`, patch otherwise. `patch`, `minor` or `major` forces it. |

### Secrets

| Secret | Required | What it does |
| --- | --- | --- |
| `KEYSTORE_BASE64` | Yes | The release keystore, base64-encoded on one line. Decoded to a temporary file during the run and deleted at the end. |
| `KEY_STORE_PASSWORD` | Yes | The keystore password. |
| `KEY_PASSWORD` | No | The key password. Leave it unset when it's the same as the keystore password. |
| `ALIAS` | Yes | The key alias in the keystore. |

Use the keystore that signed the previous releases, the one `SIGNING_KEY` points to in your local `.env`. Android refuses to update an app signed with another key: users would have to uninstall Tadami and lose their library to install the new version. Keep a backup of that file.

### Variables

| Variable | Default | What it does |
| --- | --- | --- |
| `RELEASE_DRAFT` | Unset: drafts | `false` publishes each release right away, so the in-app updater offers it immediately. Unset or any other value creates a draft. Users don't see a draft until you publish it: Releases > the draft > Edit > Publish release, keeping "Set as the latest release" checked for a normal version. |
| `RELEASE_CERT_SHA256` | Unset: no check | The SHA-256 digest of the signing certificate. When set, the release fails if the APK is signed with another certificate, before anything is published. Colons and letter case don't matter. |

### Local `.env` (builds on your PC)

Copy `.env.example` to `.env` (gitignored). Only needed to sign builds locally; without it, debug builds use the Android debug key and release builds are unsigned.

| Key | What it does |
| --- | --- |
| `SIGNING_KEY` | Path to the release keystore, absolute or relative to `app/`. `TADAMI_KEYSTORE_PATH` also works and wins if both are set (it's the name the Release workflow uses). |
| `KEY_STORE_PASSWORD` | Keystore password. |
| `KEY_PASSWORD` | Key password; defaults to `KEY_STORE_PASSWORD` when empty. |
| `ALIAS` | Key alias. |

Real environment variables override `.env`. When the keystore exists, debug builds are signed with it too, so a debug build installed from your machine keeps its signature across reinstalls.

---

## Extensions repo (tadamiorg/tadami-extensions-source)

### Workflow

| Workflow | File | Runs on |
| --- | --- | --- |
| Release extensions | `.github/workflows/release-extensions.yml` | Only by hand: Actions > Release extensions > Run workflow |

It builds only the extensions you name, then commits each one's APK, icon and index entry to tadami-extensions. Every other extension stays as it is, including the ones left out of the index on purpose. Before changing anything, it refuses an extension:
- whose version code isn't above the published one;
- whose APK is signed with another key than the published one;
- whose code uses an extension API member its lib version doesn't cover, for example `SAnime.language` without `apiVersion` 1.6. Older apps only accept lib versions up to their own max, so the lib version is what keeps them from loading code they'd crash on. Two runs never overlap. The built APKs are attached to every run as the `extensions` artifact.

### Run options

| Input | Default | What it does |
| --- | --- | --- |
| `extensions` | (required) | The extensions to release, as `<lang>.<name>` separated by spaces or commas, e.g. `en.animepahe fr.franime`. The names are the folders under `extensions/`. An unknown name stops the run before building. |
| `dry_run` | Off | Builds, checks and prepares the release in the extensions repo, but doesn't push it. The run summary and the `extensions` artifact show what would be released. |

Bump `extVersionCode` in the extension's `build.gradle.kts` before releasing it. Releasing an extension that is currently left out of the index lists it again, so only release extensions that work.

### Secrets

| Secret | Required | What it does |
| --- | --- | --- |
| `EXTENSIONS_KEYSTORE_BASE64` | Yes | The keystore that signed the published extensions (`KEY_FILE` in the extensions repo's `.env`), base64-encoded on one line. |
| `EXTENSIONS_KEY_STORE_PASSWORD` | Yes | Its password. The build also uses it as the key password, so the key must have the same password as the keystore. |
| `EXTENSIONS_KEY_ALIAS` | Yes | The key alias (`ALIAS` in `.env`). |
| `EXTENSIONS_REPO_TOKEN` | Yes, except for dry runs | A token that may push to tadamiorg/tadami-extensions. The built-in workflow token can only write to its own repo. |

Same rule as the app: use the key that signed the published extensions, or users can't update them. The workflow checks it and refuses a mismatch.

Creating `EXTENSIONS_REPO_TOKEN`: GitHub > your avatar > Settings > Developer settings > Personal access tokens > **Fine-grained tokens** > Generate new token:
- **Resource owner**: `tadamiorg`
- **Repository access**: Only select repositories > `tadami-extensions`
- **Permissions** > Repository permissions > **Contents: Read and write** (Metadata: Read-only is added automatically)
- An expiration date you'll remember: when it expires, releases fail at the push step until you replace the secret.

If the organization requires approval for fine-grained tokens, an owner approves it in the organization's Settings > Personal access tokens. Your account needs write access to tadami-extensions.

### Variables

None.

### Environment variables used inside the workflow

You don't set these; they're listed so the build scripts make sense.

| Variable | Set by | What it does |
| --- | --- | --- |
| `CI` | GitHub (`true`) | Puts `settings.gradle.kts` in CI mode. |
| `CI_EXTENSIONS` | The `extensions` input | In CI mode, `settings.gradle.kts` loads only these extensions, and an unknown name fails immediately. |
| `KEY_FILE`, `KEY_STORE_PASSWORD`, `ALIAS` | The secrets | Read by `common.gradle` to sign the APKs, the same names as in your `.env`. |
| `CI_CHUNK_SIZE`, `CI_CHUNK_NUM` | Nobody | Legacy chunked builds of every extension. Used only when `CI_EXTENSIONS` is empty; without them, CI mode loads all extensions. |

### Local `.env`

| Key | What it does |
| --- | --- |
| `KEY_FILE` | Path to the extensions keystore. |
| `KEY_STORE_PASSWORD` | Its password, also used as the key password. |
| `ALIAS` | Key alias. |

---

## Setting them

### In the browser

Repo > **Settings > Secrets and variables > Actions**, then **New repository secret** (Secrets tab) or **New repository variable** (Variables tab). Paste the value as is, with no quotes.

### With the GitHub CLI

Leaving out `--body` makes `gh` prompt for the value, which keeps passwords out of your shell history.

```powershell
# App
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\release.jks")) | gh secret set KEYSTORE_BASE64 --repo tadamiorg/tadami
gh secret set KEY_STORE_PASSWORD --repo tadamiorg/tadami
gh secret set ALIAS --repo tadamiorg/tadami
gh variable set RELEASE_CERT_SHA256 --repo tadamiorg/tadami --body "<sha-256 digest>"
gh variable set RELEASE_DRAFT --repo tadamiorg/tadami --body false   # only to stop using drafts

# Extensions
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\path\to\extensions.jks")) | gh secret set EXTENSIONS_KEYSTORE_BASE64 --repo tadamiorg/tadami-extensions-source
gh secret set EXTENSIONS_KEY_STORE_PASSWORD --repo tadamiorg/tadami-extensions-source
gh secret set EXTENSIONS_KEY_ALIAS --repo tadamiorg/tadami-extensions-source
gh secret set EXTENSIONS_REPO_TOKEN --repo tadamiorg/tadami-extensions-source
```

On Linux, encode with `base64 -w0 release.jks`; on macOS, `base64 -i release.jks`.

### Getting the certificate digest for `RELEASE_CERT_SHA256`

Either from an APK signed with the key (Android SDK build-tools):

```sh
apksigner verify --print-certs tadami.apk     # the "certificate SHA-256 digest" line
```

or from the keystore (JDK):

```sh
keytool -list -v -keystore release.jks -alias <alias>     # the SHA256 line under "Certificate fingerprints"
```

## Checking the setup

- **App**: with drafts on (the default), a real run is a safe test: it ends with a draft nobody sees. Check the run is green and the draft holds the APK, then publish it, or delete the draft and the tag it pushed (`git push --delete origin vX.Y.Z`), or the next release skips that version.
- **Extensions**: run Release extensions on one extension with **dry_run** ticked. It builds, checks the signing key against the published APK and prepares the index without pushing anything. If that's green, the only thing left untested is the push, which needs `EXTENSIONS_REPO_TOKEN`.
