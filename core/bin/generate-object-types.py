#!/usr/bin/env python3

import os
import pybars
import yaml


root = os.path.dirname(os.path.realpath(__file__)) + "/.."
types_path = f"{root}/bin/object-types.yaml"
enum_hbs_path = f"{root}/src/main/java/core/util/ObjectType.java.hbs"
enum_path = f"{root}/src/main/java/core/util/ObjectType.java"
converter_hbs_path = f"{root}/src/main/java/core/util/ObjectTypeConverter.java.hbs"
converter_path = f"{root}/src/main/java/core/util/ObjectTypeConverter.java"

with open(types_path) as types_yaml:
    data = yaml.load(types_yaml.read(), Loader=yaml.SafeLoader)
    with open(enum_hbs_path) as enum_hbs, open(enum_path, "w") as enum:
        enum.write(
            pybars.Compiler().compile(enum_hbs.read())(
                {"object_types": ",\n    ".join(data["object_types"].keys())}
            )
        )

    with open(converter_hbs_path) as converter_hbs, open(converter_path, "w") as converter:
        converter.write(
            pybars.Compiler().compile(converter_hbs.read())(
                {
                    "named_object_types": [
                        {"upper": e, "lower": e.lower()} for e in data["named_objects"]
                    ],
                    "object_types": [
                        {"upper": e, "lower": e.lower()}
                        for e in set(data["object_types"].keys()) - set(["PHRASE"])
                    ],
                }
            )
        )
