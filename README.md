# MarkDoc

MarkDoc is a Markdown->HTML Documentation Converter in Clojure.

The purpose is to allow multiple formats of the exact same documentation to exist at build time. Markdown for ease-of-use in a repository, such as on GitHub, and HTML for publishing with distributions or hosting the documentation online.

## Usage

MarkDoc takes a directory of Markdown files (`:in-root`), a target directory for the HTML (`:out-root`) and (optionally) the location of a [Selmer](https://github.com/yogthos/Selmer) template (`:template-file`) with which to process the Markdown. Running MarkDoc looks like the following:

```
clj -X:convert :in-root '"[dir of .md files]"' \
:out-root '"[HTML destination dir]"' \
:template-file '"[selmer template file]"'
```

## Templates

See [Selmer](https://github.com/yogthos/Selmer) for full template usage documentation. By default MarkDoc provides two variables to the template at runtime:

`{{content}}` - The rendered HTML from a given MD file.

`{{sha}}` - If MarkDoc is run from within a Git repository for the project it's being used on this will contain the current Git SHA. Potentially useful for tracking doc versions. Otherwise this tag will hold the default value "No SHA found."

You can also add your own variables for the template by using the optional argument `:template-vars` and passing in a k/v map of the arguments in [EDN](https://github.com/edn-format/edn) syntax. The example below passes in the `version` variable which can be printed in the template using `{{version}}`:

```
clj -X:convert :in-root '"doc/"' \
:out-root '"html/"' \
:template-file '"templates/docs.html.template"' \
:template-vars '{:version "v0.0.2"}'
```

Take care NOT to use the key `:content`

## License

Copyright © 2021 Yet Analytics, Inc.

Distributed under the Apache License version 2.0.
