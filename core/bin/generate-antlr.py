#!/usr/bin/env python3

import os
import pybars
import yaml

ABERRANT_PLURAL_MAP = {
    "appendix": "appendices",
    "barracks": "barracks",
    "cactus": "cacti",
    "child": "children",
    "criterion": "criteria",
    "deer": "deer",
    "echo": "echoes",
    "elf": "elves",
    "embargo": "embargoes",
    "focus": "foci",
    "fungus": "fungi",
    "goose": "geese",
    "hero": "heroes",
    "hoof": "hooves",
    "index": "indices",
    "knife": "knives",
    "leaf": "leaves",
    "life": "lives",
    "man": "men",
    "mouse": "mice",
    "nucleus": "nuclei",
    "person": "people",
    "phenomenon": "phenomena",
    "potato": "potatoes",
    "self": "selves",
    "syllabus": "syllabi",
    "tomato": "tomatoes",
    "torpedo": "torpedoes",
    "veto": "vetoes",
    "woman": "women",
}

VOWELS = set("aeiou")


def pluralize(singular):
    """Return plural form of given lowercase singular word (English only). Based on
    ActiveState recipe http://code.activestate.com/recipes/413172/

    >>> pluralize('')
    ''
    >>> pluralize('goose')
    'geese'
    >>> pluralize('dolly')
    'dollies'
    >>> pluralize('genius')
    'genii'
    >>> pluralize('jones')
    'joneses'
    >>> pluralize('pass')
    'passes'
    >>> pluralize('zero')
    'zeros'
    >>> pluralize('casino')
    'casinos'
    >>> pluralize('hero')
    'heroes'
    >>> pluralize('church')
    'churches'
    >>> pluralize('x')
    'xs'
    >>> pluralize('car')
    'cars'

    """
    if not singular:
        return ""
    plural = ABERRANT_PLURAL_MAP.get(singular)
    if plural:
        return plural
    root = singular
    try:
        if singular[-1] == "y" and singular[-2] not in VOWELS:
            root = singular[:-1]
            suffix = "ies"
        elif singular[-1] == "s":
            if singular[-2] in VOWELS:
                if singular[-3:] == "ius":
                    root = singular[:-2]
                    suffix = "i"
                else:
                    root = singular[:-1]
                    suffix = "ses"
            else:
                suffix = "es"
        elif singular[-2:] in ("ch", "sh"):
            suffix = "es"
        else:
            suffix = "s"
    except IndexError:
        suffix = "s"
    plural = root + suffix
    return plural


def to_camel_case(string):
    words = string.split("_")
    return words[0].lower() + "".join([w.lower().capitalize() for w in words[1:]])


def create_rulelist(object_types, object_id, make_plural=False):
    rules = []

    flattened = sorted(
        [
            (token, value)
            for token, values in object_types.items()
            for value in (values if isinstance(values, list) else [values])
        ],
        reverse=True,
        key=lambda e: len(e[1].split(" ")),
    )

    for token, value in flattened:
        pluralized = pluralize(value) if make_plural else value
        rule_name = to_camel_case("%s_%s_object\n" % (token.lower(), object_id.lower()))
        if rule_name not in rules:
            rules.append(rule_name)

    return rules


def create_distinct_rules(object_types, object_id, make_plural=False):
    rules = []

    flattened = sorted(
        [
            (token, value)
            for token, values in object_types.items()
            for value in (values if isinstance(values, list) else [values])
        ],
        reverse=True,
        key=lambda e: len(e[1].split(" ")),
    )
    rules_dict = {}

    for token, value in flattened:
        pluralized = pluralize(value) if make_plural else value
        rule_name = to_camel_case(f"{token.lower()}_{object_id.lower()}_object")
        if rule_name in rules_dict:
            rules_dict[rule_name] = f"{rules_dict[rule_name]} | {pluralized.upper()}"
        else:
            rules_dict[rule_name] = f"{pluralized.upper()}"
    for key, value in rules_dict.items():
        rules.append(f"{key} : {value} ; \n")

    return rules


def create_tokens(object_types):
    rules = set()
    for token, values in object_types.items():
        if not isinstance(values, list):
            values = [values]

        for value in values:
            for word in value.split(" "):
                pluralized = pluralize(word)
                rules.add("%s : '%s' ;" % (word.upper(), word.lower()))
                rules.add("%s : '%s' ;" % (pluralized.upper(), pluralized.lower()))

    return rules


if __name__ == "__main__":
    root = os.path.dirname(os.path.realpath(__file__))
    antlr = f"{root}/../src/main/antlr"
    output = f"{root}/../src/main/resources"

    with open(f"{root}/../bin/object-types.yaml") as object_types_yaml, open(
        f"{antlr}/CommandLexer.g4.hbs"
    ) as lexer_hbs, open(f"{output}/CommandLexer.g4", "w") as lexer_g4, open(
        f"{antlr}/CommandParser.g4.hbs"
    ) as parser_hbs, open(
        f"{output}/CommandParser.g4", "w"
    ) as parser_g4:
        joiner = " | "
        data = yaml.load(object_types_yaml.read(), Loader=yaml.SafeLoader)

        lexer_g4.write(
            pybars.Compiler().compile(lexer_hbs.read())(
                {"generated_tokens": "\n".join(create_tokens(data["object_types"]))}
            )
        )

        excluded_selection_objects = set(data["excluded_selection_objects"])
        parser_g4.write(
            pybars.Compiler().compile(parser_hbs.read())(
                {
                    "selection_objects_singular": joiner.join(
                        create_rulelist(
                            {
                                k: v
                                for k, v in data["object_types"].items()
                                if k not in excluded_selection_objects
                            },
                            "singular",
                        )
                    ),
                    "selection_object_singular_rules": "".join(
                        create_distinct_rules(
                            {
                                k: v
                                for k, v in data["object_types"].items()
                                if k not in excluded_selection_objects
                            },
                            "singular",
                        )
                    ),
                    "selection_objects_plural": joiner.join(
                        create_rulelist(
                            {
                                k: v
                                for k, v in data["object_types"].items()
                                if k not in excluded_selection_objects
                            },
                            "plural",
                            make_plural=True,
                        )
                    ),
                    "selection_object_plural_rules": "".join(
                        create_distinct_rules(
                            {
                                k: v
                                for k, v in data["object_types"].items()
                                if k not in excluded_selection_objects
                            },
                            "plural",
                            make_plural=True,
                        )
                    ),
                    "named_selection_object": joiner.join(
                        create_rulelist(
                            {e: data["object_types"][e] for e in data["named_objects"]},
                            "named",
                        )
                    ),
                    "selection_object_named_rules": "".join(
                        create_distinct_rules(
                            {e: data["object_types"][e] for e in data["named_objects"]},
                            "named",
                        )
                    ),
                }
            )
        )
