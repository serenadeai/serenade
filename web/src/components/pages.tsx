import React from "react";
import { Helmet } from "react-helmet";
import { Block } from "./block";
import { Footer } from "./footer";
import { DocsNavigation, MainNavigation } from "./navigation";
import { TableOfContents } from "./docs/table-of-contents";
import { Heading, Subheading, Link as DocsLink } from "./docs/headings";

const Page: React.FC<{ title: string }> = ({ children, title }) => (
  <>
    <Helmet>
      <title>{title || "Serenade | Code with voice"}</title>
      <meta
        name="description"
        content="Serenade is the most powerful way to program using natural speech. Boost your productivity by adding voice to your workflow."
        key="meta-description"
      />
      <meta
        name="thumbnail"
        content="https://cdn.serenade.ai/web/img/logo-background.png"
        key="meta-thumbnail"
      />
      <meta name="theme-color" content="#ffffff" key="meta-theme-color" />
      <meta property="og:url" content="https://serenade.ai" key="meta-og:url" />
      <meta property="og:title" content="Serenade" key="meta-og:title" />
      <meta
        name="og:description"
        content="Serenade is the most powerful way to program using natural speech. Boost your productivity by adding voice to your workflow."
        key="meta-og:description"
      />
      <meta property="og:site_name" content="Serenade" key="meta-og:site_name" />
      <meta
        property="og:image"
        content="https://cdn.serenade.ai/web/img/logo-background.png"
        key="meta-og:image"
      />
      <script
        async
        src="https://www.googletagmanager.com/gtag/js?id=UA-137730125-1"
        key="gtag"
      ></script>
      <script defer src="/lib.js" key="lib"></script>
    </Helmet>
    {children}
  </>
);

export const Docs: React.FC<{ tableOfContents: TableOfContents; title: string }> = ({
  tableOfContents,
  title,
}) => (
  <Page>
    <div className="docs-page">
      <DocsNavigation />
      <div className="flex">
        <nav
          className="hidden md:block bg-gray-100 border-r border-gray-300 px-8 py-4"
          style={{ height: "calc(100vh - 60px)", width: "250px", overflowY: "scroll" }}
        >
          {tableOfContents.sections.map((section) => (
            <div key={section.title}>
              <DocsLink
                title={section.title}
                className="font-normal text-gray-500 text-lg block mb-3 mt-6 hover:text-purple-500 transition-colors"
              />
              {section.subsections.map((subsection) => (
                <div key={subsection.title}>
                  <DocsLink
                    title={subsection.title}
                    className="font-light text-slate-600 block my-2 hover:text-purple-500 transition-colors"
                  />
                </div>
              ))}
            </div>
          ))}
        </nav>
        <main className="py-4 px-6 docs-content">
          {tableOfContents.sections.map((section) => (
            <div key={section.title}>
              <Heading title={section.title} />
              {section.content ? section.content : null}
              {section.subsections.map((subsection) => (
                <div key={subsection.title}>
                  <Subheading title={subsection.title} />
                  {subsection.content}
                </div>
              ))}
            </div>
          ))}
        </main>
      </div>
    </div>
  </Page>
);

export const Gray: React.FC = ({ children }) => (
  <Page>
    <div className="min-h-screen flex flex-col bg-gray-100">
      <nav>
        <MainNavigation hideBorder={true} />
      </nav>
      <main className="flex-1 flex flex-col">{children}</main>
      <Footer />
    </div>
  </Page>
);

export const Legal: React.FC<{ title: string }> = ({ children, title }) => (
  <Page>
    <>
      <nav>
        <MainNavigation />
      </nav>
      <main className="legal-page bg-gray-100">
        <div className="py-12">
          <Block>
            <h1 className="font-bold text-4xl pb-4">{title}</h1>
            {children}
          </Block>
        </div>
      </main>
      <Footer />
    </>
  </Page>
);

export const Main: React.FC = ({ children }) => (
  <Page>
    <>
      <nav>
        <MainNavigation />
      </nav>
      <main className="overflow-x-hidden">
        {children}
        <Footer />
      </main>
    </>
  </Page>
);
