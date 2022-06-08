import os
import re
import yaml

base_url = "s3://serenadecdn.com" if os.getenv("SERENADE_USE_S3") else "https://serenadecdn.com"


def _expand_paths(data, defaults):
    for k, v in data.items():
        if isinstance(v, dict):
            _expand_paths(v, defaults)
        elif isinstance(v, str):
            data[k] = re.sub(
                r"\$([A-Za-z_]+)",
                lambda e: defaults.get(e.group(1), os.getenv(e.group(1)) or ""),
                v.replace("~", os.path.expanduser("~")),
            )


def _merge(a, b):
    for k in b.keys():
        if k in a and isinstance(a[k], dict) and isinstance(b[k], dict):
            _merge(a[k], b[k])
        else:
            a[k] = b[k]


def languages():
    with open(source_path("config", "languages.yaml")) as f:
        return yaml.load(f, Loader=yaml.SafeLoader)


def library_path(*args):
    return os.path.join(
        os.getenv("SERENADE_LIBRARY_ROOT") or os.path.expanduser("~/libserenade"), *args
    )


def models():
    with open(source_path("config", "models.yaml")) as f:
        return yaml.load(f, Loader=yaml.SafeLoader)


def services(env=None):
    data = {}
    with open(source_path("config", "services.yaml")) as f:
        data.update(yaml.load(f, Loader=yaml.SafeLoader))

    keys = library_path("keys.yaml")
    if os.path.exists(keys):
        with open(keys) as f:
            _merge(data, yaml.load(f, Loader=yaml.SafeLoader))

    _merge(data, os.environ)
    _expand_paths(
        data,
        {
            "SERENADE_SOURCE_ROOT": source_path(),
            "SERENADE_LIBRARY_ROOT": library_path(),
        },
    )

    return data if not env else {**data["default"], **data[env]}


def source_path(*args):
    return os.path.join(
        os.getenv("SERENADE_SOURCE_ROOT")
        or os.path.join(os.path.dirname(os.path.realpath(__file__)), "..", ".."),
        *args
    )
