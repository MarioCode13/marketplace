#!/usr/bin/env python3
import sys,hashlib,urllib.parse

def md5hex(s):
    return hashlib.md5(s.encode('utf-8')).hexdigest()

def rfc3986_encode(s):
    if s is None:
        return ''
    return urllib.parse.quote(s, safe='~').replace('+','%20')

def canonical_base(params, include_merchant_key=True, url_encode_values=False, passphrase=None, excluded_keys=None):
    if excluded_keys is None:
        excluded_keys = set()
    p = {k:v for k,v in params.items() if v is not None and v != '' and k not in excluded_keys}
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
        params[k] = urllib.parse.unquote_plus(v[0]) if v else ''
    return params

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print('Usage: python compute_payfast_signature_bruteforce.py "<full url with signature>" <passphrase>')
        sys.exit(1)
    url = sys.argv[1]
    passphrase = sys.argv[2]
    params = parse_url(url)
    provided = params.get('signature')
    if 'signature' in params:
        params.pop('signature')
    print('Provided signature:', provided)

    optional_keys = ['cancel_url','return_url','notify_url','custom_str1','custom_str2','recurring_amount','cycles','email_address','item_name','frequency']
    # We'll try excluding any subset of these (2^n combos up to reasonable size). Limit subsets to up to 3 exclusions to keep runtime small
    from itertools import combinations

    tested = 0
    matches = []
    for include_key in (True, False):
        for url_encoded in (False, True):
            # Try excluding 0..3 keys
            for r in range(0,4):
                for combo in combinations(optional_keys, r):
                    excluded = set(combo)
                    base = canonical_base(params, include_merchant_key=include_key, url_encode_values=url_encoded, passphrase=passphrase, excluded_keys=excluded)
                    sig = md5hex(base)
                    tested += 1
                    if sig == provided:
                        matches.append((include_key, url_encoded, excluded, base, sig))
    print(f'Tested variants: {tested}')
    if matches:
        print('Matches found:')
        for m in matches:
            print('---')
            print('include_merchant_key=', m[0])
            print('url_encoded=', m[1])
            print('excluded_keys=', m[2])
            print('BASE:', m[3])
            print('MD5 :', m[4])
    else:
        print('No matches found among tried variants (excluded combos up to size 3).')
    print('\nDone')

