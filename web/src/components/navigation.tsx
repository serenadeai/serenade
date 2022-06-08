import React, { Fragment } from "react";
import classNames from "classnames";
import { Link } from "gatsby";
import { Popover, Transition } from "@headlessui/react";
import {
  ArrowsExpandIcon,
  BookOpenIcon,
  ChatIcon,
  CodeIcon,
  DatabaseIcon,
  DocumentTextIcon,
  HeartIcon,
  LightningBoltIcon,
  MenuIcon,
  RssIcon,
  UserGroupIcon,
  XIcon,
} from "@heroicons/react/outline";
import { ChevronDownIcon } from "@heroicons/react/solid";
import { useStore } from "../lib/store";

const Dropdown: React.FC<{
  content: React.FC;
  right?: boolean;
  last?: boolean;
  text: string;
}> = ({ content, last, right, text }) => (
  <Popover className="relative inline-block">
    {({ open }) => (
      <>
        <Popover.Button
          className={classNames(
            "group bg-white rounded-md inline-flex items-center text-base font-medium hover:text-gray-900 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500",
            {
              "text-gray-900": open,
              "text-gray-500": !open,
            }
          )}
        >
          <span>{text}</span>
          <ChevronDownIcon
            className={classNames("ml-2 h-5 w-5 group-hover:text-gray-500", {
              "text-gray-600": open,
              "text-gray-400": !open,
            })}
            aria-hidden="true"
          />
        </Popover.Button>

        <Transition
          as={Fragment}
          enter="transition ease-out duration-200"
          enterFrom="opacity-0 translate-y-1"
          enterTo="opacity-100 translate-y-0"
          leave="transition ease-in duration-150"
          leaveFrom="opacity-100 translate-y-0"
          leaveTo="opacity-0 translate-y-1"
        >
          {right ? (
            <Popover.Panel className="absolute z-10 right-0 mt-3 px-2 w-screen max-w-sm md:max-w-md sm:px-0">
              {content}
            </Popover.Panel>
          ) : last ? (
            <Popover.Panel className="absolute z-10 left-1/2 transform -translate-x-1/2 mt-3 px-2 w-screen max-w-md sm:px-0">
              {content}
            </Popover.Panel>
          ) : (
            <Popover.Panel className="absolute z-10 -ml-4 mt-3 transform px-2 w-screen max-w-sm md:max-w-md sm:px-0 lg:ml-0 lg:left-1/2 lg:-translate-x-1/2">
              {content}
            </Popover.Panel>
          )}
        </Transition>
      </>
    )}
  </Popover>
);

const IconNavigationDropdown: React.FC<{
  items: { description: string; icon: any; href: string; title: string }[];
  last?: boolean;
  text: string;
}> = ({ items, last, text }) => (
  <Dropdown
    last={last}
    text={text}
    content={
      <div className="rounded-lg shadow-lg ring-1 ring-black ring-opacity-5 overflow-hidden">
        <div className="relative grid gap-6 bg-white px-5 py-6 sm:gap-8 sm:p-8">
          {items.map((item) => (
            <a
              key={item.title}
              href={item.href}
              className="-m-3 p-3 flex items-start rounded-lg hover:bg-gray-50"
            >
              <item.icon className="flex-shrink-0 h-6 w-6 text-purple-600" aria-hidden="true" />
              <div className="ml-4">
                <p className="text-base font-medium text-gray-900">{item.title}</p>
                <p className="mt-1 text-sm text-gray-500">{item.description}</p>
              </div>
            </a>
          ))}
        </div>
      </div>
    }
  />
);

const MobileNavigationLink: React.FC<{ href: string; icon: any; text: string }> = ({
  href,
  icon,
  text,
}) => (
  <Link to={href} className="-m-3 p-3 flex items-center rounded-md hover:bg-gray-50">
    <icon.icon className="flex-shrink-0 h-6 w-6 text-purple-600" aria-hidden="true" />
    <span className="ml-3 text-base font-medium text-gray-900">{text}</span>
  </Link>
);

const NavigationLink: React.FC<{ href: string; text: string }> = ({ href, text }) => (
  <Link to={href} className="text-base font-medium text-gray-500 hover:text-gray-900">
    {text}
  </Link>
);

const TextNavigationDropdown: React.FC<{
  items: string[];
  last?: boolean;
  onClick: (e: React.MouseEvent, item: string) => void;
  right?: boolean;
  text: string;
  twoColumns?: boolean;
}> = ({ items, last, onClick, right, text, twoColumns }) => (
  <Dropdown
    last={last}
    right={right}
    text={text}
    content={
      <div className="rounded-lg shadow-lg ring-1 ring-black ring-opacity-5 overflow-hidden">
        <div
          className={classNames("relative grid gap-6 bg-white px-5 py-6 sm:gap-8 sm:p-8", {
            "grid-cols-2": twoColumns,
          })}
        >
          {items.map((item) => (
            <a
              href="#"
              key={item}
              onClick={(e) => onClick(e, item)}
              className="-m-3 p-3 flex items-start rounded-lg hover:bg-gray-50"
            >
              <p className="text-base font-medium text-gray-900">{item}</p>
            </a>
          ))}
        </div>
      </div>
    }
  />
);

const More = () => (
  <IconNavigationDropdown
    last={true}
    text="More"
    items={[
      {
        title: "Community",
        description: "Get help, share feedback, and meet other voice coders",
        href: "/community",
        icon: ChatIcon,
      },
      {
        title: "Plugins",
        description: "Integrate Serenade with your favorite tools",
        href: "/plugins",
        icon: ArrowsExpandIcon,
      },
      {
        title: "Blog",
        description: "Updates from the Serenade team",
        href: "/blog",
        icon: RssIcon,
      },
    ]}
  />
);

const UseCases = () => (
  <IconNavigationDropdown
    items={[
      {
        title: "Health & RSI",
        description:
          "Alleviate pain in your wrists, neck, back, and more by programming without typing.",
        href: "/health",
        icon: HeartIcon,
      },
      {
        title: "Full Stack",
        description: "Use Serenade across the stack, from React & CSS to Python & Java",
        href: "/fullstack",
        icon: CodeIcon,
      },
      {
        title: "Backend",
        description: "Write your backend and infrastructure code in Go, Rust, C++, and more",
        href: "/backend",
        icon: DatabaseIcon,
      },
      {
        title: "Documents",
        description: "Write more than code—use Serenade for docs, PRs, and chat",
        href: "/everywhere",
        icon: DocumentTextIcon,
      },
    ]}
    text="Use Cases"
  />
);

export const DocsNavigation = () => {
  const { state, dispatch } = useStore();

  return (
    <div className="w-screen border-b border-gray-300 flex items-center" style={{ height: "60px" }}>
      <Link to="/">
        <span className="sr-only">Serenade</span>
        <img
          src="https://cdn.serenade.ai/web/img/wordmark.svg"
          alt="Serenade Logo"
          className="px-8"
          style={{ width: "180px" }}
        />
      </Link>
      <div className="flex-grow"></div>
      <div className="px-8">
        <span className="hidden md:inline-block mr-3">
          <NavigationLink href="/docs" text="Editor" />
        </span>
        <span className="hidden md:inline-block mr-3">
          <NavigationLink href="/docs/browser" text="Browser" />
        </span>
        <span className="hidden md:inline-block mr-3">
          <NavigationLink href="/docs/api" text="Custom Commands" />
        </span>
        <span className="hidden md:inline-block mr-3">
          <NavigationLink href="/docs/protocol" text="Custom Plugins" />
        </span>
        <TextNavigationDropdown
          items={[
            "C / C++",
            "C#",
            "CSS",
            "Dart",
            "Go",
            "HTML",
            "Java",
            "JavaScript",
            "Kotlin",
            "Python",
            "Ruby",
            "Rust",
            "TypeScript",
          ]}
          onClick={(e, item) => {
            e.preventDefault();
            dispatch({
              type: "language",
              language: item,
            });
          }}
          right={true}
          text={state.language}
          twoColumns={true}
        />
      </div>
    </div>
  );
};

export const MainNavigation: React.FC<{ hideBorder?: boolean }> = ({ hideBorder }) => (
  <Popover className="relative bg-white">
    <div className="max-w-7xl mx-auto px-4 sm:px-6">
      <div
        className={classNames(
          "flex justify-between items-center py-4 md:justify-start md:space-x-10",
          {
            "border-b-2 border-gray-100": !hideBorder,
          }
        )}
      >
        <div className="flex justify-start lg:w-0 lg:flex-1">
          <Link to="/">
            <span className="sr-only">Serenade</span>
            <img
              src="https://cdn.serenade.ai/web/img/wordmark.svg"
              alt="Serenade Logo"
              className="w-32"
            />
          </Link>
        </div>
        <div className="-mr-2 -my-2 md:hidden">
          <Popover.Button className="bg-white rounded-md p-2 inline-flex items-center justify-center text-gray-400 hover:text-gray-500 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-purple-500">
            <span className="sr-only">Open menu</span>
            <MenuIcon className="h-6 w-6" aria-hidden="true" />
          </Popover.Button>
        </div>
        <Popover.Group as="nav" className="hidden md:flex space-x-10">
          <UseCases />
          <NavigationLink href="/docs" text="Docs" />
          <NavigationLink href="https://github.com/serenadeai/serenade" text="GitHub" />
          <More />
        </Popover.Group>
        <div className="hidden md:flex items-center justify-end md:flex-1 lg:w-0">
          <Link to="/download" className="primary-button font-medium">
            Download
          </Link>
        </div>
      </div>
    </div>

    <Transition
      as={Fragment}
      enter="transition ease-out duration-200"
      enterFrom="opacity-0 translate-y-1"
      enterTo="opacity-100 translate-y-0"
      leave="transition ease-in duration-150"
      leaveFrom="opacity-100 translate-y-0"
      leaveTo="opacity-0 translate-y-1"
    >
      <Popover.Panel
        focus
        className="absolute z-10 top-0 inset-x-0 p-2 transition transform origin-top-right md:hidden"
      >
        <div className="rounded-lg shadow-lg ring-1 ring-black ring-opacity-5 bg-white divide-y-2 divide-gray-50">
          <div className="pt-5 pb-6 px-5">
            <div className="flex items-center justify-between">
              <div className="-mr-2">
                <Popover.Button className="bg-white rounded-md p-2 inline-flex items-center justify-center text-gray-400 hover:text-gray-500 hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-500">
                  <span className="sr-only">Close menu</span>
                  <XIcon className="h-6 w-6" aria-hidden="true" />
                </Popover.Button>
              </div>
            </div>
            <div className="mt-6">
              <nav className="grid gap-y-8">
                <MobileNavigationLink icon={{ icon: BookOpenIcon }} href="/docs" text="Docs" />
                <MobileNavigationLink
                  icon={{ icon: LightningBoltIcon }}
                  href="/pro"
                  text="Pricing"
                />
                <MobileNavigationLink
                  icon={{ icon: ChatIcon }}
                  href="/community"
                  text="Community"
                />
                <MobileNavigationLink
                  icon={{ icon: HeartIcon }}
                  href="/health"
                  text="Health & RSI"
                />
                <MobileNavigationLink
                  icon={{ icon: CodeIcon }}
                  href="/fullstack"
                  text="Full Stack"
                />
                <MobileNavigationLink
                  icon={{ icon: DatabaseIcon }}
                  href="/backend"
                  text="Backend"
                />
                <MobileNavigationLink icon={{ icon: RssIcon }} href="/blog" text="Blog" />
              </nav>
            </div>
          </div>
        </div>
      </Popover.Panel>
    </Transition>
  </Popover>
);
