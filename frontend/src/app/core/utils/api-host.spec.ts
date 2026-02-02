import { resolveApiBase, resolveApiHost, resolveApiProtocol, __setMockLocation } from './api-host';

describe('api host utilities', () => {
  afterEach(() => __setMockLocation());

  it('should fall back to defaults when no window available', () => {
    expect(resolveApiHost()).toBe('localhost');
    expect(resolveApiProtocol()).toBe('http:');
    expect(resolveApiBase(8080)).toBe('http://localhost:8080');
  });

  it('should read values from runtime location', () => {
    __setMockLocation({ hostname: 'app.local', protocol: 'https:' } as Location);

    expect(resolveApiHost()).toBe('app.local');
    expect(resolveApiProtocol()).toBe('https:');
    expect(resolveApiBase(4200)).toBe('https://app.local:4200');
  });

  it('should handle protocol with trailing colon', () => {
    __setMockLocation({ hostname: 'example.com', protocol: 'https:' } as Location);

    expect(resolveApiBase(3000)).toBe('https://example.com:3000');
  });

  it('should clear mock location', () => {
    __setMockLocation({ hostname: 'first.com', protocol: 'https:' } as Location);
    expect(resolveApiHost()).toBe('first.com');

    __setMockLocation(undefined);
    expect(resolveApiHost()).toBe('localhost');
  });

  it('should handle partial location object', () => {
    __setMockLocation({ hostname: 'partial.com' } as Location);

    expect(resolveApiHost()).toBe('partial.com');
    expect(resolveApiProtocol()).toBe('http:');
  });
});
