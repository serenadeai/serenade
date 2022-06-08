export type Step = {
  title: string;
  body: string;
  transcript?: string;
  matches?: string[];
  source?: string;
  cursor?: number;
  skipEditorFocus?: boolean;
  nextWhenEditorFilename?: string;
  nextWhenEditorFocused?: string;
  index?: number;
  last?: boolean;
  error?: boolean;
  hideAnswer?: boolean;
  textOnly?: boolean;
};

export type Tutorial = {
  filename?: string;
  steps: Step[];
};

export const tutorials: {
  title: string;
  tutorial: string;
  description: string;
  basic?: boolean;
}[] = [
  {
    title: "Python Basics",
    tutorial: "python-basics",
    basic: true,
    description: "Get started with Python",
  },
  {
    title: "JavaScript Basics",
    tutorial: "javascript-basics",
    basic: true,
    description: "Get started with JavaScript/TypeScript",
  },
  {
    title: "Java Basics",
    tutorial: "java-basics",
    basic: true,
    description: "Get started with Java",
  },
  {
    title: "C/C++ Basics",
    tutorial: "cplusplus-basics",
    basic: true,
    description: "Get started with C and C++",
  },
  {
    title: "C# Basics",
    tutorial: "csharp-basics",
    basic: true,
    description: "Get started with C#",
  },
  {
    title: "Ruby Basics",
    tutorial: "ruby-basics",
    basic: true,
    description: "Get started with Ruby",
  },
  {
    title: "Go Basics",
    tutorial: "go-basics",
    basic: true,
    description: "Get started with Go",
  },
  {
    title: "Rust Basics",
    tutorial: "rust-basics",
    basic: true,
    description: "Get started with Rust",
  },
  {
    title: "Chrome Basics",
    tutorial: "chrome-basics",
    description: "Learn how to navigate the web",
  },
  {
    title: "Formatting",
    tutorial: "formatting",
    description: "Learn text formatting commands with JavaScript",
  },
  {
    title: "Navigation",
    tutorial: "navigation",
    description: "Learn how to navigate code with Python",
  },
  {
    title: "Advanced Python",
    tutorial: "python-advanced",
    description: "Learn some advanced Python concepts",
  },
  {
    title: "Advanced JavaScript",
    tutorial: "javascript-advanced",
    description: "Learn some advanced JavaScript concepts",
  },
];
