const DEFAULT_HOST = 'localhost';
const DEFAULT_PROTOCOL = 'http:';
let mockedLocation: Location | null = null;

function getRuntimeLocation(): Location | undefined {
  if (mockedLocation) {
    return mockedLocation;
  }

  if (typeof globalThis === 'undefined') {
    return undefined;
  }

  const runtime = globalThis as typeof globalThis & {
    location?: Location;
    window?: { location?: Location };
  };

  return runtime.location ?? runtime.window?.location;
}

/**
 * Utility exposed for tests to simulate browser locations.
 */
export function __setMockLocation(location?: Partial<Location>) {
  mockedLocation = location ? (location as Location) : null;
}

export function resolveApiHost(): string {
  const location = getRuntimeLocation();
  return location?.hostname ?? DEFAULT_HOST;
}

export function resolveApiProtocol(): string {
  const location = getRuntimeLocation();
  return location?.protocol ?? DEFAULT_PROTOCOL;
}

export function resolveApiBase(port: number): string {
  const protocol = resolveApiProtocol().replace(/:$/, '');
  const host = resolveApiHost();
  return `${protocol}://${host}:${port}`;
}
