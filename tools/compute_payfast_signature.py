#!/usr/bin/env python3
import sys,hashlib,urllib.parse

def md5hex(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

def rfc3986_encode(s):
    if s is None:
        return ''
    return urllib.parse.quote(s, safe='~').replace('+','%20')

def canonical_base(params, include_merchant_key=True, url_encode_values=False, passphrase=None):
    p = {k:v for k,v in params.items() if v is not None and v != ''}
    if not include_merchant_key:
        p.pop('merchant_key', None)
    keys = sorted(p.keys())
    parts = []
    for k in keys:
        v = p[k]
        val = rfc3986_encode(v) if url_encode_values else v
        parts.append(f"{k}={val}")
    base = "&".join(parts)
    if passphrase:
        base = base + "&passphrase=" + passphrase
    return base

def parse_url(url):
    u = urllib.parse.urlparse(url)
    qs = urllib.parse.parse_qs(u.query, keep_blank_values=True)
    params = {}
    for k,v in qs.items():
        # preserve raw decoded value (plus -> space); keep percent-decoded
        params[k] = urllib.parse.unquote_plus(v[0]) if v else ''
    return params

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print('Usage: python compute_payfast_signature.py "<full url with signature>" <passphrase>')
        sys.exit(1)
    url = sys.argv[1]
    passphrase = sys.argv[2]
    params = parse_url(url)
    provided = params.get('signature')
    if 'signature' in params:
        params.pop('signature')
    print('Provided signature:', provided)
    print('\nParsed params (decoded):')
    for k in sorted(params.keys()):
        print(f'  {k} = {params[k]}')
    print('\nVariants:')
    variants = [
        (True, False, 'raw_with_key'),
        (True, True, 'encoded_with_key'),
        (False, False, 'raw_without_key'),
        (False, True, 'encoded_without_key'),
    ]
    for include_key, encoded, name in variants:
        base = canonical_base(params, include_merchant_key=include_key, url_encode_values=encoded, passphrase=passphrase)
        sig = md5hex(base)
        print(f'--- {name} ---')
        print('BASE:', base)
        print('MD5 :', sig)
        print('MATCH?:', 'YES' if provided == sig else 'NO')
        print()

