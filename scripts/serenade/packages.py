import boto3
import os.path
import requests
import time
import serenade.config

default_session = None
sessions = {}


def _parse_s3_url(url):
    url = url[len("s3://") :]
    return url.split("/")[0], url[url.index("/") + 1 :]


def _session(bucket):
    global default_session, sessions

    if os.path.exists(os.path.expanduser("~/.aws/credentials")):
        sessions.setdefault(bucket, boto3.Session(profile_name=bucket))
        return sessions[bucket]

    if not default_session:
        default_session = boto3.Session()

    return default_session


def download(url, path):
    if url.startswith("s3://"):
        s3_bucket, s3_path = _parse_s3_url(url)
        _session(s3_bucket).client("s3").download_file(s3_bucket, s3_path, path)
    else:
        with requests.get(url, allow_redirects=True, stream=True) as r:
            r.raise_for_status()
            with open(path, "wb") as f:
                for chunk in r.iter_content(chunk_size=8192):
                    f.write(chunk)


def upload(path, url):
    if url.startswith("s3://"):
        s3_bucket, s3_path = _parse_s3_url(url)
        _session(s3_bucket).client("s3").upload_file(path, s3_bucket, s3_path)
