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

## Configuration

The API URL is configured in `shadow-cljs.edn`:

- Development environment: Uses `http://localhost:3000` by default
- Production environment: Configure your production API URL in the `:release` section of shadow-cljs.edn

## License

Copyright © Joseph Graham
