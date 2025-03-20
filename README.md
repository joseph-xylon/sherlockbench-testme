# SherlockBench Human Client

This codebase is a web-application which allows a human to take the
SherlockBench AI benchmark. It connects to the API presented by the
SherlockBench API server.

## Main Website
The project homepage: https://sherlockbench.com

## Technical info
This app is written in ClojureScript with the following stack:
- reitit for routing
- replicant for rendering
- portfolio for UI testing
- serialized EDN in localstorage for state

## Configuration

The only config is the API URL which is configured in `shadow-cljs.edn`:

- Development environment: Uses `http://localhost:3000` by default
- Production environment: Configure your production API URL in the `:release` section of shadow-cljs.edn

## Development

Update node with: `npm update`

For local development:
```
npx shadow-cljs watch app
```

This will use the development environment configuration with `http://localhost:3000` as the API URL.

## Production Build

To build for production:
```
npx shadow-cljs release app
```

This will use the production environment configuration with the production API URL configured in shadow-cljs.edn.

## License

Copyright © Joseph Graham
