#!/usr/bin/env python3

import os
import subprocess
import serenade.config
from datetime import datetime


def evaluate_model():
    """Evaluate model and generate a metric report"""
    model = serenade.packaging.config.models()["speech-engine"]
    output_name = f"{datetime.now().strftime('%Y-%m-%d_%H%M%S')}_{model['model']}"
    output_path = os.path.join(serenade.config.library_path("evaluation"), output_name)
    os.makedirs(output_path, exist_ok=True)
    serenade.packaging.models.download_speech_engine(model)

    replayer_path = serenade.config.source_path(
        "scripts", "replayer", "build", "install", "replayer", "bin", "replayer"
    )

    subprocess.check_call(
        f"""{replayer_path} \
            --audio "{serenade.config.library_path("audio")}" \
            --output "{output_path}" \
            --test-set {serenade.config.library_path("test-set.jsonl")}
        """,
        shell=True,
        env={
            **os.environ,
            **{
                "CODE_ENGINE_HOST": "localhost",
                "CODE_ENGINE_PORT": "17203",
                "ENV": "dev",
            },
        },
    )
    print(f"Results located in {output_path}")


if __name__ == "__main__":
    evaluate_model()
