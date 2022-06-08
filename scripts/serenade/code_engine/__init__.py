import serenade.config


def data_path(*args):
    return serenade.config.library_path("code-engine-training", "data", *args)


def model_path(*args):
    return serenade.config.library_path("code-engine-training", "models", *args)
