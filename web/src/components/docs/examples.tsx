export type Example =
  | "addArgument"
  | "addClass"
  | "addComment"
  | "addDecorator"
  | "addElseIf"
  | "addEnum"
  | "addFormatting"
  | "addFunction"
  | "addIf"
  | "addImport"
  | "addLambda"
  | "addMethod"
  | "addParameter"
  | "addProperty"
  | "addStatement"
  | "addSymbol"
  | "addWhile"
  | "changeObject"
  | "changeText"
  | "comment"
  | "copy"
  | "cut"
  | "dedent"
  | "deleteLine"
  | "deleteRange"
  | "deleteSelector"
  | "deleteToEndpoint"
  | "duplicateBlock"
  | "duplicateFunction"
  | "goToIndex"
  | "goToLine"
  | "goToObject"
  | "goToText"
  | "indent"
  | "insertAbove"
  | "insertEnclosure"
  | "insertEscape"
  | "insertFormatting"
  | "insertSimple"
  | "joinLines"
  | "move"
  | "paste"
  | "rename"
  | "select"
  | "shift"
  | "surround"
  | "systemSimple"
  | "textStyleMultiple"
  | "textStyleSimple"
  | "uncomment";

export const examplesByLanguage: { [key: string]: { [key in Example]?: string[] } } = {
  All: {
    addFormatting: ["add argument pascal some class", "create(<b>SomeClass</b>)"],
    addSymbol: ["insert this dot get of key", "this.get(key)"],
    copy: ["copy next two words", "<mark></mark>all <i>too well</i>"],
    cut: ["cut to end of line", "this is <mark></mark><i>a long line of text</i>"],
    dedent: ["dedent line", "    y = f(x)\nz = g(y)", "y = f(x)\nz = g(y)"],
    duplicateBlock: ["duplicate block", "value++\nprint(value);\n<b>value++\nprint(value);</b>"],
    indent: ["indent line", "y = f(x)\nz = g(y)", "    y = f(x)\nz = g(y)"],
    insertAbove: ["insert above one", "two<mark></mark>", "<b>one</b>\ntwo"],
    insertEnclosure: ["insert first equals array brackets zero", "first = array[0]"],
    insertEscape: ["insert escape plus", "plus"],
    insertFormatting: [
      "insert camel my age equals snake your age plus one",
      "myAge = your_age + 1",
    ],
    insertSimple: ["insert hello world", "hello world"],
    joinLines: ["join five lines", "list = [\n  1,\n  2,\n  3\n]", "list = [1, 2, 3]"],
    move: ["move argument left", "z = f(x, y)", "z = f(y, x)"],
    paste: ["paste", ""],
    select: ["select previous word", "<i>shake</i> <mark></mark>it off"],
    shift: ["shift line down", 'first = "a";\nsecond = "b";', 'second = "b";\nfirst = "a";'],
    surround: [
      "surround value with quotes",
      "message = Hello world",
      'message = <b>"</b>Hello world<b>"</b>',
    ],
    systemSimple: ["system simple text", "simple text"],
    textStyleMultiple: ["camel case next two words", "one two three", "<b>oneTwo</b> three"],
    textStyleSimple: ["capitalize foo", "foo", "<b>F</b>oo"],
  },
  "C / C++": {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>);"],
    addClass: ["add class page", "class Page {\n}"],
    addComment: ["add comment fix later", "// fix later"],
    addElseIf: [
      "add else if x less than three",
      "if (x > 3) {\n  return;\n}\n<b>else if (x < 3) {\n}</b>",
    ],
    addEnum: ["add enum colors", "enum Colors {\n}"],
    addFunction: ["add function int factorial", "int factorial() {\n}"],
    addIf: ["add if not error", "if (!error) {\n}"],
    addImport: ["add include iostream", "#include &lt;iostream&gt;"],
    addMethod: ["add method string name", "class Person {\n  <b>string name() {\n  }</b>\n}"],
    addParameter: ["add parameter int speed", "void move(<b>int speed</b>) {\n}"],
    addProperty: [
      "add float property radius equals three point five",
      "class Circle {\n  <b>float radius = 3.5;</b>\n}",
    ],
    addStatement: ["add return say of string hello", 'return say("hello");'],
    addWhile: ["add while i not equal zero", "while (i != 0) {\n}"],
    changeObject: [
      "change argument to string background",
      'setColor("foreground", "blue");',
      'setColor(<b>"background"</b>, "blue");',
    ],
    changeText: [
      "change height to speed",
      "void fly(int height) {\n}",
      "void fly(int <b>speed</b>) {\n}",
    ],
    comment: ["comment function", "<b>//</b> int random() {\n<b>//</b>  return 4;\n<b>//</b>}"],
    deleteLine: [
      "delete line four",
      "class Bird {\n  void fly() {\n    // TODO\n    <u>return;</u>\n  }\n}",
    ],
    deleteRange: [
      "delete lines three to four",
      "class Bird {\n  void fly() {\n    <u>// TODO\n    return;</u>\n  }\n}",
    ],
    deleteSelector: [
      "delete method",
      "class Bird {\n  <u>void fly() {\n    // TODO\n    return;</u>\n  }\n}",
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "class B<mark></mark><u>ird</u> {\n  void fly() {\n    // TODO\n    return;\n  }\n}",
    ],
    duplicateFunction: [
      "duplicate function",
      "void initialize() {\n  buffer.reset();\n}\n<b>void initialize() {\n  buffer.reset();\n}</b>",
    ],
    goToIndex: [
      "go to second parameter",
      "int random(int low, <mark></mark>int high) {\n  return 4;\n}",
    ],
    goToLine: ["go to line two", "int random(int low, int high) {\n<mark></mark>  return 4;\n}"],
    goToObject: [
      "go to start of function",
      "<mark></mark>int random(int low, int high) {\n  return 4;\n}",
    ],
    goToText: ["go to random", "int <mark></mark>random(int low, int high) {\n  return 4;\n}"],
    rename: ["rename class to setup", "class Install {\n}", "class <b>Setup</b> {\n}"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai";\n<u>//</u> pages = 3;\n<u>//</u> download(url, pages);',
    ],
  },
  "C#": {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>);"],
    addClass: ["add class page", "class Page {\n}"],
    addComment: ["add comment fix later", "// fix later"],
    addDecorator: ["add attribute serializable", "<b>[Serializable]</b>\npublic class Data {\n}"],
    addElseIf: [
      "add else if x less than three",
      "if (x > 3) {\n  return;\n}\n<b>else if (x < 3) {\n}</b>",
    ],
    addEnum: ["add enum colors", "enum Colors {\n}"],
    addFunction: ["add method public int factorial", "public int factorial() {\n}"],
    addIf: ["add if not error", "if (!error) {\n}"],
    addLambda: ["add callback equals lambda e", "callback = (e) => {\n};"],
    addMethod: ["add method string name", "class Person {\n  <b>String name() {\n  }</b>\n}"],
    addParameter: ["add parameter int speed", "void move(<b>int speed</b>) {\n}"],
    addProperty: [
      "add protected float property radius equals three point five",
      "class Circle {\n  <b>protected float radius = 3.5;</b>\n}",
    ],
    addStatement: ["add return say of string hello", 'return say("hello");'],
    addWhile: ["add while i not equal zero", "while (i != 0) {\n}"],
    changeObject: [
      "change argument to string background",
      'setColor("foreground", "blue");',
      'setColor(<b>"background"</b>, "blue");',
    ],
    changeText: [
      "change height to speed",
      "void fly(int height) {\n}",
      "void fly(int <b>speed</b>) {\n}",
    ],
    comment: ["comment method", "<b>//</b> int random() {\n<b>//</b>  return 4;\n<b>//</b>}"],
    deleteLine: [
      "delete line four",
      "class Bird {\n  void fly() {\n    // TODO\n    <u>return;</u>\n  }\n}",
    ],
    deleteRange: [
      "delete lines three to four",
      "class Bird {\n  void fly() {\n    <u>// TODO\n    return;</u>\n  }\n}",
    ],
    deleteSelector: [
      "delete method",
      "class Bird {\n  <u>void fly() {\n    // TODO\n    return;</u>\n  }\n}",
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "class B<mark></mark><u>ird</u> {\n  void fly() {\n    // TODO\n    return;\n  }\n}",
    ],
    duplicateFunction: [
      "duplicate method",
      "void initialize() {\n  buffer.reset();\n}\n<b>void initialize() {\n  buffer.reset();\n}</b>",
    ],
    goToIndex: [
      "go to second parameter",
      "int random(int low, <mark></mark>int high) {\n  return 4;\n}",
    ],
    goToLine: ["go to line two", "int random(int low, int high) {\n<mark></mark>  return 4;\n}"],
    goToObject: [
      "go to start of method",
      "<mark></mark>int random(int low, int high) {\n  return 4;\n}",
    ],
    goToText: ["go to random", "int <mark></mark>random(int low, int high) {\n  return 4;\n}"],
    rename: ["rename class to setup", "class Install {\n}", "class <b>Setup</b> {\n}"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai";\n<u>//</u> pages = 3;\n<u>//</u> download(url, pages);',
    ],
  },
  CSS: {
    addComment: ["add comment fix later", "// fix later"],
    addFunction: ["add ruleset dot login", ".login {\n}"],
    addStatement: ["add background colon blue", "background: blue;"],
    changeText: [
      "change blue to red",
      ".overlay {\n  background: blue;\n}",
      ".overlay {\n  background: red;\n}",
    ],
    comment: ["comment block", "<b>//</b> a {\n<b>//</b>  display: block;\n<b>//</b>}"],
    deleteLine: ["delete line two", "#hero {\n  <u>display: block;</u>\n}"],
    deleteRange: [
      "delete lines two to three",
      "img {\n  <u>display: block;\n  margin: 0 auto;</u>\n}",
    ],
    deleteToEndpoint: ["delete to end of word", ".hi<mark></mark><u>dden</u> {\n}"],
    duplicateFunction: [
      "duplicate property",
      "#app {\n  color: #16161d;\n  <b>color: #16161d;</b>\n}",
    ],
    goToLine: ["go to line two", "h1 {\n  <mark></mark>font-size: 2rem;\n}"],
    goToText: ["go to background", ".container {\n  <mark></mark>background: blue;\n}"],
    uncomment: ["uncomment three lines", "<u>//</u> a {\n<u>//</u>  display: block;\n<u>//</u>}"],
  },
  Dart: {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>);"],
    addClass: ["add class page", "class Page {\n}"],
    addComment: ["add comment fix later", "// fix later"],
    addElseIf: [
      "add else if x less than three",
      "if (x > 3) {\n  return;\n}\n<b>else if (x < 3) {\n}</b>",
    ],
    addEnum: ["add enum colors", "enum Colors {\n}"],
    addFunction: ["add function int factorial", "int factorial() {\n}"],
    addIf: ["add if not error", "if (!error) {\n}"],
    addImport: ["add import dart colon math", 'import "dart:math"'],
    addMethod: ["add method string name", "class Person {\n  <b>String name() {\n  }</b>\n}"],
    addParameter: ["add parameter int speed", "void move(<b>int speed</b>) {\n}"],
    addProperty: [
      "add float property radius equals three point five",
      "class Circle {\n  <b>float radius = 3.5;</b>\n}",
    ],
    addStatement: ["add return say of string hello", 'return say("hello");'],
    addWhile: ["add while i not equal zero", "while (i != 0) {\n}"],
    changeObject: [
      "change argument to string background",
      'setColor("foreground", "blue");',
      'setColor(<b>"background"</b>, "blue");',
    ],
    changeText: [
      "change height to speed",
      "void fly(int height) {\n}",
      "void fly(int <b>speed</b>) {\n}",
    ],
    comment: ["comment function", "<b>//</b> int random() {\n<b>//</b>  return 4;\n<b>//</b>}"],
    deleteLine: [
      "delete line four",
      "class Bird {\n  void fly() {\n    // TODO\n    <u>return;</u>\n  }\n}",
    ],
    deleteRange: [
      "delete lines three to four",
      "class Bird {\n  void fly() {\n    <u>// TODO\n    return;</u>\n  }\n}",
    ],
    deleteSelector: [
      "delete method",
      "class Bird {\n  <u>void fly() {\n    // TODO\n    return;</u>\n  }\n}",
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "class B<mark></mark><u>ird</u> {\n  void fly() {\n    // TODO\n    return;\n  }\n}",
    ],
    duplicateFunction: [
      "duplicate function",
      "void initialize() {\n  buffer.reset();\n}\n<b>void initialize() {\n  buffer.reset();\n}</b>",
    ],
    goToIndex: [
      "go to second parameter",
      "int random(int low, <mark></mark>int high) {\n  return 4;\n}",
    ],
    goToLine: ["go to line two", "int random(int low, int high) {\n<mark></mark>  return 4;\n}"],
    goToObject: [
      "go to start of function",
      "<mark></mark>int random(int low, int high) {\n  return 4;\n}",
    ],
    goToText: ["go to random", "int <mark></mark>random(int low, int high) {\n  return 4;\n}"],
    rename: ["rename class to setup", "class Install {\n}", "class <b>Setup</b> {\n}"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai";\n<u>//</u> pages = 3;\n<u>//</u> download(url, pages);',
    ],
  },
  Go: {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>);"],
    addComment: ["add comment fix later", "// fix later"],
    addElseIf: [
      "add else if x less than three",
      "if x > 3 {\n  return;\n}\n<b>else if x < 3 {\n}</b>",
    ],
    addFunction: ["add function int factorial", "func factorial() int {\n}"],
    addIf: ["add if not error", "if !error {\n}"],
    addImport: ["add import string fmt", 'import "fmt"'],
    addLambda: ["add callback equals lambda e int", "callback = func(e int) -> {\n};"],
    addMethod: [
      "add method string name",
      "type person interface {\n  <b>name() string {\n  }</b>\n}",
    ],
    addParameter: ["add parameter int speed", "func move(<b>speed int</b>) {\n}"],
    addProperty: ["add int property players", "type game struct {\n  <b>players int</b>\n}"],
    addStatement: ["add return say of string hello", 'return say("hello")'],
    addWhile: ["add while i not equal zero", "while i != 0 {\n}"],
    changeObject: [
      "change argument to string background",
      'setColor("foreground", "blue");',
      'setColor(<b>"background"</b>, "blue");',
    ],
    changeText: [
      "change height to speed",
      "func fly(height int) {\n}",
      "func fly(<b>speed</b> int) {\n}",
    ],
    comment: ["comment function", "<b>//</b> func random() {\n<b>//</b>  return 4\n<b>//</b>}"],
    deleteLine: ["delete line three", "func fly() {\n  // TODO\n  <u>return;</u>\n}"],
    deleteRange: ["delete lines three to four", "func fly() {\n  <u>// TODO\n  return</u>\n}"],
    deleteSelector: [
      "delete function",
      'import "fmt"\n<u>func fly() {\n  // TODO\n  return</u>\n}',
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "func fly() {\n  // TODO\n  re<mark></mark><u>turn</u>;\n}",
    ],
    duplicateFunction: [
      "duplicate function",
      "func initialize() {\n  buffer.reset()\n}\n<b>func initialize() {\n  buffer.reset();\n}</b>",
    ],
    goToIndex: [
      "go to second parameter",
      "func random(low int, <mark></mark>high int) {\n  return 4\n}",
    ],
    goToLine: ["go to line two", "func random(low int, high int) {\n<mark></mark>  return 4\n}"],
    goToObject: [
      "go to start of method",
      "<mark></mark>int random(int low, int high) {\n  return 4;\n}",
    ],
    goToText: ["go to random", "func <mark></mark>random(low int, high int) int {\n  return 4\n}"],
    rename: ["rename function to setup", "func install() {\n}", "func <b>setup</b>() {\n}"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai"\n<u>//</u> pages = 3\n<u>//</u> download(url, pages)',
    ],
  },
  HTML: {
    addComment: ["add comment fix later", "&lt;!-- fix later --&gt;"],
    addFunction: ["add attribute id equals string login", '&lt;button <b>id="login"</b>&gt;'],
    addStatement: ["add tag div", "&lt;div&gt;&lt;/div&gt;"],
    comment: ["comment block", "<b>&lt;!--</b> &lt;h1&gt;Page Title&lt;/h1&gt; <b>--&gt;</b>"],
    uncomment: ["uncomment line", "<u>&lt;!--</u> &lt;h1&gt;Page Title&lt;/h1&gt; <u>--&gt;</u>"],
  },
  Java: {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>);"],
    addClass: ["add class page", "class Page {\n}"],
    addComment: ["add comment fix later", "// fix later"],
    addDecorator: ["add annotation override", "<b>@Override</b>\npublic String toString() {\n}"],
    addElseIf: [
      "add else if x less than three",
      "if (x > 3) {\n  return;\n}\n<b>else if (x < 3) {\n}</b>",
    ],
    addEnum: ["add enum colors", "enum Colors {\n}"],
    addFunction: ["add method public int factorial", "public int factorial() {\n}"],
    addIf: ["add if not error", "if (!error) {\n}"],
    addImport: ["add import java dot util dot list", "import java.util.List;"],
    addLambda: ["add callback equals lambda e", "callback = (e) -> {\n};"],
    addMethod: ["add method string name", "class Person {\n  <b>String name() {\n  }</b>\n}"],
    addParameter: ["add parameter int speed", "void move(<b>int speed</b>) {\n}"],
    addProperty: [
      "add protected float property radius equals three point five",
      "class Circle {\n  <b>protected float radius = 3.5;</b>\n}",
    ],
    addStatement: ["add return say of string hello", 'return say("hello");'],
    addWhile: ["add while i not equal zero", "while (i != 0) {\n}"],
    changeObject: [
      "change argument to string background",
      'setColor("foreground", "blue");',
      'setColor(<b>"background"</b>, "blue");',
    ],
    changeText: [
      "change height to speed",
      "void fly(int height) {\n}",
      "void fly(int <b>speed</b>) {\n}",
    ],
    comment: ["comment method", "<b>//</b> int random() {\n<b>//</b>  return 4;\n<b>//</b>}"],
    deleteLine: [
      "delete line four",
      "class Bird {\n  void fly() {\n    // TODO\n    <u>return;</u>\n  }\n}",
    ],
    deleteRange: [
      "delete lines three to four",
      "class Bird {\n  void fly() {\n    <u>// TODO\n    return;</u>\n  }\n}",
    ],
    deleteSelector: [
      "delete method",
      "class Bird {\n  <u>void fly() {\n    // TODO\n    return;</u>\n  }\n}",
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "class B<mark></mark><u>ird</u> {\n  void fly() {\n    // TODO\n    return;\n  }\n}",
    ],
    duplicateFunction: [
      "duplicate method",
      "void initialize() {\n  buffer.reset();\n}\n<b>void initialize() {\n  buffer.reset();\n}</b>",
    ],
    goToIndex: [
      "go to second parameter",
      "int random(int low, <mark></mark>int high) {\n  return 4;\n}",
    ],
    goToLine: ["go to line two", "int random(int low, int high) {\n<mark></mark>  return 4;\n}"],
    goToObject: [
      "go to start of method",
      "<mark></mark>int random(int low, int high) {\n  return 4;\n}",
    ],
    goToText: ["go to random", "int <mark></mark>random(int low, int high) {\n  return 4;\n}"],
    rename: ["rename class to setup", "class Install {\n}", "class <b>Setup</b> {\n}"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai";\n<u>//</u> pages = 3;\n<u>//</u> download(url, pages);',
    ],
  },
  JavaScript: {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>);"],
    addClass: ["add class page", "class Page {\n}"],
    addComment: ["add comment fix later", "// fix later"],
    addElseIf: [
      "add else if x less than three",
      "if (x > 3) {\n  return;\n}\n<b>else if (x < 3) {\n}</b>",
    ],
    addFunction: ["add function factorial", "function factorial() {\n}"],
    addIf: ["add if not error", "if (!error) {\n}"],
    addLambda: ["add callback equals lambda e", "callback = () => {\n};"],
    addMethod: ["add method name", "class Person {\n  <b>name() {\n  }</b>\n}"],
    addParameter: ["add parameter speed", "function move(<b>speed</b>) {\n}"],
    addProperty: [
      "add property radius equals three point five",
      "class Circle {\n  <b>radius = 3.5;</b>\n}",
    ],
    addStatement: ["add return say of string hello", 'return say("hello");'],
    addWhile: ["add while i not equal zero", "while (i != 0) {\n}"],
    changeObject: [
      "change argument to string background",
      'setColor("foreground", "blue");',
      'setColor(<b>"background"</b>, "blue");',
    ],
    changeText: [
      "change height to speed",
      "function fly(height) {\n}",
      "function fly(<b>speed</b>) {\n}",
    ],
    comment: [
      "comment function",
      "<b>//</b> function random() {\n<b>//</b>  return 4;\n<b>//</b>}",
    ],
    deleteLine: [
      "delete line four",
      "class Bird {\n  fly() {\n    // TODO\n    <u>return;</u>\n  }\n}",
    ],
    deleteRange: [
      "delete lines three to four",
      "class Bird {\n  fly() {\n    <u>// TODO\n    return;</u>\n  }\n}",
    ],
    deleteSelector: [
      "delete method",
      "class Bird {\n  <u>fly() {\n    // TODO\n    return;</u>\n  }\n}",
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "class B<mark></mark><u>ird</u> {\n  fly() {\n    // TODO\n    return;\n  }\n}",
    ],
    duplicateFunction: [
      "duplicate function",
      "function initialize() {\n  buffer.reset();\n}\n<b>function initialize() {\n  buffer.reset();\n}</b>",
    ],
    goToIndex: [
      "go to second parameter",
      "function random(low, <mark></mark>high) {\n  return 4;\n}",
    ],
    goToLine: ["go to line two", "function random(low, high) {\n<mark></mark>  return 4;\n}"],
    goToObject: [
      "go to start of function",
      "<mark></mark>function random(low, high) {\n  return 4;\n}",
    ],
    goToText: ["go to random", "function <mark></mark>random(low, high) {\n  return 4;\n}"],
    rename: ["rename class to setup", "class Install {\n}", "class <b>Setup</b> {\n}"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai";\n<u>//</u> pages = 3;\n<u>//</u> download(url, pages);',
    ],
  },
  Kotlin: {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>);"],
    addClass: ["add class page", "class Page {\n}"],
    addComment: ["add comment fix later", "// fix later"],
    addElseIf: [
      "add else if x less than three",
      "if (x > 3) {\n  return;\n}\n<b>else if (x < 3) {\n}</b>",
    ],
    addEnum: ["add enum colors", "enum Colors {\n}"],
    addFunction: ["add int function factorial", "fun factorial(): int {\n}"],
    addIf: ["add if not error", "if (!error) {\n}"],
    addImport: ["add import foo dot bar", "import foo.bar"],
    addMethod: ["add string method name", "class Person {\n  <b>fun name(): String {\n  }</b>\n}"],
    addParameter: ["add parameter int speed", "fun move(<b>speed: Int</b>) {\n}"],
    addProperty: [
      "add property radius equals three point five",
      "class Circle {\n  <b>radius = 3.5</b>\n}",
    ],
    addStatement: ["add return say of string hello", 'return say("hello")'],
    addWhile: ["add while i not equal zero", "while (i != 0) {\n}"],
    changeObject: [
      "change argument to string background",
      'setColor("foreground", "blue")',
      'setColor(<b>"background"</b>, "blue")',
    ],
    changeText: [
      "change height to speed",
      "fun fly(height: Int) {\n}",
      "fun fly(<b>speed</b>: Int) {\n}",
    ],
    comment: ["comment function", "<b>//</b> fun random(): Int {\n<b>//</b>  return 4\n<b>//</b>}"],
    deleteLine: [
      "delete line four",
      "class Bird {\n  fun fly() {\n    // TODO\n    <u>return</u>\n  }\n}",
    ],
    deleteRange: [
      "delete lines three to four",
      "class Bird {\n  fun fly() {\n    <u>// TODO\n    return</u>\n  }\n}",
    ],
    deleteSelector: [
      "delete method",
      "class Bird {\n  <u>fun fly() {\n    // TODO\n    return</u>\n  }\n}",
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "class B<mark></mark><u>ird</u> {\n  fun fly() {\n    // TODO\n    return\n  }\n}",
    ],
    duplicateFunction: [
      "duplicate method",
      "fun initialize() {\n  buffer.reset();\n}\n<b>fun initialize() {\n  buffer.reset()\n}</b>",
    ],
    goToIndex: [
      "go to second parameter",
      "fun random(low: Int, <mark></mark>high: Int): Int {\n  return 4\n}",
    ],
    goToLine: [
      "go to line two",
      "fun random(low: Int, high: Int): Int {\n<mark></mark>  return 4\n}",
    ],
    goToObject: [
      "go to start of function",
      "<mark></mark>fun random(low: Int, high: Int) {\n  return 4\n}",
    ],
    goToText: [
      "go to random",
      "fun <mark></mark>random(low: Int, high: Int): Int {\n  return 4\n}",
    ],
    rename: ["rename class to setup", "class Install {\n}", "class <b>Setup</b> {\n}"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai"\n<u>//</u> pages = 3\n<u>//</u> download(url, pages)',
    ],
  },
  Python: {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>)"],
    addClass: ["add class page", "class Page:\n  pass"],
    addComment: ["add comment fix later", "# fix later"],
    addDecorator: ["add decorator app dot route", "<b>@app.route</b>\ndef index():\n  pass"],
    addElseIf: ["add else if x less than three", "if x > 3:\n  return\n<b>elif x < 3:\n  pass</b>"],
    addEnum: ["add enum colors", "class Colors(enum.Enum):\n  pass"],
    addFunction: ["add function int factorial", "def factorial() -> int:\n  pass"],
    addIf: ["add if not error", "if not error:\n  pass"],
    addImport: ["add import numpy as np", "import numpy as np"],
    addLambda: ["add callback equals lambda e", "callback = lambda e: pass"],
    addMethod: ["add str method name", "class Person:\n  <b>def name(self) -> str:\n    pass</b>"],
    addParameter: ["add parameter int speed", "def move(<b>speed: int</b>):\n  pass"],
    addProperty: [
      "add property radius equals three point five",
      "class Circle:\n  <b>radius = 3.5</b>",
    ],
    addStatement: ["add return say of string hello", 'return say("hello")'],
    addWhile: ["add while i not equal zero", "while i != 0:\n  pass"],
    changeObject: [
      "change argument to string background",
      'set_color("foreground", "blue")',
      'set_color(<b>"background"</b>, "blue")',
    ],
    changeText: [
      "change height to speed",
      "def fly(height: int):\n  pass",
      "def fly(<b>speed</b>: int):\n  pass",
    ],
    comment: ["comment function", "<b>#</b> def random():\n<b>#</b>  return 4"],
    deleteLine: [
      "delete line four",
      "class Bird:\n  def fly(self):\n    // TODO\n    <u>return</u>",
    ],
    deleteRange: [
      "delete lines three to four",
      "class Bird:\n  def fly(self):\n    <u>// TODO\n    return</u>",
    ],
    deleteSelector: [
      "delete method",
      "class Bird:\n  <u>def fly(self):\n    // TODO\n    return</u>",
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "class B<mark></mark><u>ird</u>:\n  def fly(self):\n    // TODO\n    return",
    ],
    duplicateFunction: [
      "duplicate function",
      "def initialize():\n  buffer.reset()\n<b>def initialize():\n  buffer.reset()</b>",
    ],
    goToIndex: ["go to second parameter", "def random(low, <mark></mark>high):\n  return 4"],
    goToLine: ["go to line two", "def random(low, high):\n<mark></mark>  return 4"],
    goToObject: ["go to start of function", "<mark></mark>def random(low, high):\n  return 4"],
    goToText: ["go to random", "def <mark></mark>random(low, high):\n  return 4"],
    rename: ["rename class to setup", "class Install:\n  pass", "class <b>Setup</b>:\n  pass"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai"\n<u>//</u> pages = 3\n<u>//</u> download(url, pages)',
    ],
  },
  Ruby: {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>)"],
    addClass: ["add class page", "class Page\nend"],
    addComment: ["add comment fix later", "# fix later"],
    addElseIf: [
      "add else if x less than three",
      "if x > 3:\n  return\n<b>elsif x < 3:\n  pass</b>",
    ],
    addFunction: ["add function factorial", "def factorial\nend"],
    addIf: ["add if not error", "if !error\nend"],
    addImport: ["add require rails", "require 'rails'"],
    addMethod: ["add method name", "class Person\n  <b>def name\n  end</b>\nend"],
    addParameter: ["add parameter speed", "def move(<b>speed</b>):\nend"],
    addProperty: [
      "add property radius equals three point five",
      "class Circle\n  <b>radius = 3.5</b>\nend",
    ],
    addStatement: ["add return say of string hello", 'return say("hello")'],
    addWhile: ["add while i not equal zero", "while i != 0\nend"],
    changeObject: [
      "change argument to string background",
      'set_color("foreground", "blue")',
      'set_color(<b>"background"</b>, "blue")',
    ],
    changeText: ["change height to speed", "def fly(height):\nend", "def fly(<b>speed</b>):\nend"],
    comment: ["comment function", "<b>#</b> def random()\n<b>#</b>  return 4\n<b>#</b>end"],
    deleteLine: [
      "delete line four",
      "class Bird\n  def fly(self)\n    // TODO\n    <u>return</u>\n  end\nend",
    ],
    deleteRange: [
      "delete lines three to four",
      "class Bird\n  def fly(self)\n    <u>// TODO\n    return</u>\n  end\nend",
    ],
    deleteSelector: [
      "delete method",
      "class Bird\n  <u>def fly(self)\n    // TODO\n    return</u>\n  end\nend",
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "class B<mark></mark><u>ird</u>\n  def fly(self)\n    // TODO\n    return\n  end\nend",
    ],
    duplicateFunction: [
      "duplicate function",
      "def initialize()\n  buffer.reset()\nend\n<b>def initialize()\n  buffer.reset()\nend</b>",
    ],
    goToIndex: ["go to second parameter", "def random(low, <mark></mark>high)\n  return 4\nend"],
    goToLine: ["go to line two", "def random(low, high)\n<mark></mark>  return 4\nend"],
    goToObject: ["go to start of function", "<mark></mark>def random(low, high)\n  return 4\nend"],
    goToText: ["go to random", "def <mark></mark>random(low, high)\n  return 4\nend"],
    rename: ["rename class to setup", "class Install\n  pass", "class <b>Setup</b>\n  pass\nend"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai"\n<u>//</u> pages = 3\n<u>//</u> download(url, pages)',
    ],
  },
  Rust: {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>);"],
    addClass: ["add struct page", "struct Page {\n}"],
    addComment: ["add comment fix later", "// fix later"],
    addElseIf: [
      "add else if x less than three",
      "if x > 3 {\n  return;\n}\n<b>else if x < 3 {\n}</b>",
    ],
    addEnum: ["add enum colors", "enum Colors {\n}"],
    addFunction: ["add function u32 factorial", "fn factorial() -> u32 {\n}"],
    addIf: ["add if not error", "if !error {\n}"],
    addImport: ["add use crate", "use crate;"],
    addMethod: ["add method string name", "impl Person {\n  <b>fn name() -> String {\n  }</b>\n}"],
    addParameter: ["add parameter u32 speed", "fn move(<b>speed: u32</b>) {\n}"],
    addProperty: [
      "add f32 property radius equals three point five",
      "struct Circle {\n  <b>radius: f32 = 3.5;</b>\n}",
    ],
    addStatement: ["add return say of string hello", 'return say("hello");'],
    addWhile: ["add while i not equal zero", "while i != 0 {\n}"],
    changeObject: [
      "change argument to string background",
      'set_color("foreground", "blue");',
      'set_color(<b>"background"</b>, "blue");',
    ],
    changeText: [
      "change height to speed",
      "fn fly(height: i32) {\n}",
      "fn fly(int <b>speed</b>: i32) {\n}",
    ],
    comment: ["comment function", "<b>//</b> fn random() {\n<b>//</b>  return 4;\n<b>//</b>}"],
    deleteLine: [
      "delete line four",
      "impl Bird {\n  fn fly() {\n    // TODO\n    <u>return;</u>\n  }\n}",
    ],
    deleteRange: [
      "delete lines three to four",
      "impl Bird {\n  fn fly() {\n    <u>// TODO\n    return;</u>\n  }\n}",
    ],
    deleteSelector: [
      "delete method",
      "impl Bird {\n  <u>fn fly() {\n    // TODO\n    return;</u>\n  }\n}",
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "impl B<mark></mark><u>ird</u> {\n  fn fly() {\n    // TODO\n    return;\n  }\n}",
    ],
    duplicateFunction: [
      "duplicate function",
      "fn initialize() {\n  buffer.reset();\n}\n<b>fn initialize() {\n  buffer.reset();\n}</b>",
    ],
    goToIndex: [
      "go to second parameter",
      "fn random(low: i32, <mark></mark>high: i32) -> i32 {\n  return 4;\n}",
    ],
    goToLine: [
      "go to line two",
      "fn random(low: i32, high: i32) -> i32 {\n<mark></mark>  return 4;\n}",
    ],
    goToObject: [
      "go to start of function",
      "<mark></mark>fn random(low: i32, high: i32) -> i32 {\n  return 4;\n}",
    ],
    goToText: [
      "go to random",
      "fn <mark></mark>random(low: i32, high: i32) -> i32 {\n  return 4;\n}",
    ],
    rename: ["rename class to setup", "impl Install {\n}", "impl <b>Setup</b> {\n}"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai";\n<u>//</u> pages = 3;\n<u>//</u> download(url, pages);',
    ],
  },
  "C#": {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>);"],
    addClass: ["add class page", "class Page {\n}"],
    addComment: ["add comment fix later", "// fix later"],
    addDecorator: ["add attribute serializable", "<b>[Serializable]</b>\npublic class Data {\n}"],
    addElseIf: [
      "add else if x less than three",
      "if (x > 3) {\n  return;\n}\n<b>else if (x < 3) {\n}</b>",
    ],
    addEnum: ["add enum colors", "enum Colors {\n}"],
    addFunction: ["add method public int factorial", "public int factorial() {\n}"],
    addIf: ["add if not error", "if (!error) {\n}"],
    addLambda: ["add callback equals lambda e", "callback = (e) => {\n};"],
    addMethod: ["add method string name", "class Person {\n  <b>String name() {\n  }</b>\n}"],
    addParameter: ["add parameter int speed", "void move(<b>int speed</b>) {\n}"],
    addProperty: [
      "add protected float property radius equals three point five",
      "class Circle {\n  <b>protected float radius = 3.5;</b>\n}",
    ],
    addStatement: ["add return say of string hello", 'return say("hello");'],
    addWhile: ["add while i not equal zero", "while (i != 0) {\n}"],
    changeObject: [
      "change argument to string background",
      'setColor("foreground", "blue");',
      'setColor(<b>"background"</b>, "blue");',
    ],
    changeText: [
      "change height to speed",
      "void fly(int height) {\n}",
      "void fly(int <b>speed</b>) {\n}",
    ],
    comment: ["comment method", "<b>//</b> int random() {\n<b>//</b>  return 4;\n<b>//</b>}"],
    deleteLine: [
      "delete line four",
      "class Bird {\n  void fly() {\n    // TODO\n    <u>return;</u>\n  }\n}",
    ],
    deleteRange: [
      "delete lines three to four",
      "class Bird {\n  void fly() {\n    <u>// TODO\n    return;</u>\n  }\n}",
    ],
    deleteSelector: [
      "delete method",
      "class Bird {\n  <u>void fly() {\n    // TODO\n    return;</u>\n  }\n}",
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "class B<mark></mark><u>ird</u> {\n  void fly() {\n    // TODO\n    return;\n  }\n}",
    ],
    duplicateFunction: [
      "duplicate method",
      "void initialize() {\n  buffer.reset();\n}\n<b>void initialize() {\n  buffer.reset();\n}</b>",
    ],
    goToIndex: [
      "go to second parameter",
      "int random(int low, <mark></mark>int high) {\n  return 4;\n}",
    ],
    goToLine: ["go to line two", "int random(int low, int high) {\n<mark></mark>  return 4;\n}"],
    goToObject: [
      "go to start of method",
      "<mark></mark>int random(int low, int high) {\n  return 4;\n}",
    ],
    goToText: ["go to random", "int <mark></mark>random(int low, int high) {\n  return 4;\n}"],
    rename: ["rename class to setup", "class Install {\n}", "class <b>Setup</b> {\n}"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai";\n<u>//</u> pages = 3;\n<u>//</u> download(url, pages);',
    ],
  },
  TypeScript: {
    addArgument: ["add argument word brackets zero", "capitalize(<b>word[0]</b>);"],
    addClass: ["add class page", "class Page {\n}"],
    addComment: ["add comment fix later", "// fix later"],
    addElseIf: [
      "add else if x less than three",
      "if (x > 3) {\n  return;\n}\n<b>else if (x < 3) {\n}</b>",
    ],
    addEnum: ["add enum colors", "enum Colors {\n}"],
    addFunction: ["add number function factorial", "function factorial(): number {\n}"],
    addIf: ["add if not error", "if (!error) {\n}"],
    addImport: ["add import react element from react", 'import { ReactElement } from "react";'],
    addLambda: ["add callback equals lambda e", "callback = (e) => {\n};"],
    addMethod: ["add string method name", "class Person {\n  <b>name(): string {\n  }</b>\n}"],
    addParameter: ["add number parameter speed", "function move(<b>speed: number</b>) {\n}"],
    addProperty: [
      "add private number property radius equals three point five",
      "class Circle {\n  <b>private radius: number = 3.5;</b>\n}",
    ],
    addStatement: ["add return say of string hello", 'return say("hello");'],
    addWhile: ["add while i not equal zero", "while (i != 0) {\n}"],
    changeObject: [
      "change argument to string background",
      'setColor("foreground", "blue");',
      'setColor(<b>"background"</b>, "blue");',
    ],
    changeText: [
      "change height to speed",
      "function fly(height: number) {\n}",
      "function fly(<b>speed</b>: number) {\n}",
    ],
    comment: [
      "comment function",
      "<b>//</b> function random(): number {\n<b>//</b>  return 4;\n<b>//</b>}",
    ],
    deleteLine: [
      "delete line four",
      "class Bird {\n  fly() {\n    // TODO\n    <u>return;</u>\n  }\n}",
    ],
    deleteRange: [
      "delete lines three to four",
      "class Bird {\n  fly() {\n    <u>// TODO\n    return;</u>\n  }\n}",
    ],
    deleteSelector: [
      "delete method",
      "class Bird {\n  <u>fly() {\n    // TODO\n    return;</u>\n  }\n}",
    ],
    deleteToEndpoint: [
      "delete to end of word",
      "class B<mark></mark><u>ird</u> {\n  fly() {\n    // TODO\n    return;\n  }\n}",
    ],
    duplicateFunction: [
      "duplicate function",
      "function initialize() {\n  buffer.reset();\n}\n<b>function initialize() {\n  buffer.reset();\n}</b>",
    ],
    goToIndex: [
      "go to second parameter",
      "function random(low, <mark></mark>high) {\n  return 4;\n}",
    ],
    goToLine: ["go to line two", "function random(low, high) {\n<mark></mark>  return 4;\n}"],
    goToObject: [
      "go to start of function",
      "<mark></mark>function random(low, high) {\n  return 4;\n}",
    ],
    goToText: ["go to random", "function <mark></mark>random(low, high) {\n  return 4;\n}"],
    rename: ["rename class to setup", "class Install {\n}", "class <b>Setup</b> {\n}"],
    uncomment: [
      "uncomment lines two to three",
      'url = "serenade.ai";\n<u>//</u> pages = 3;\n<u>//</u> download(url, pages);',
    ],
  },
};
