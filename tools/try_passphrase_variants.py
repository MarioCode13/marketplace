#!/usr/bin/env python3
import hashlib, urllib.parse, itertools

url = "https://www.payfast.co.za/eng/process?amount=49.00&cancel_url=https%3A%2F%2Fwww.dealio.org.za%2Fsubscriptions%2Fcancel&custom_str1=verified_user&custom_str2=jimi%40gmail.com&cycles=0&email_address=jimi%40gmail.com&frequency=3&item_name=Verified%20User%20Subscription&merchant_id=12247699&merchant_key=fdsyodv8kudog&name_first=Jimi&name_last=Hendrix&notify_url=https%3A%2F%2Fapi.dealio.org.za%2Fapi%2Fpayments%2Fpayfast%2Fitn&recurring_amount=49.00&return_url=https%3A%2F%2Fwww.dealio.org.za%2Fsubscriptions%2Fsuccess&subscription_type=1&signature=620356780b523e2bd93b0138dcc75c23"
qs = urllib.parse.parse_qs(urllib.parse.urlparse(url).query, keep_blank_values=True)
params = {k: urllib.parse.unquote_plus(v[0]) for k,v in qs.items()}
provided = params.pop('signature')

# base passphrase candidates: user provided and previously-seen variant
base_candidates = ["d3aL1oM4rK3T","d3ali0Mark3t","d3aLi0Mark3t","d3aL1oMark3t","d3ali0Mark3T"]
# mapping of ambiguous characters
amb_map = {
    'l': ['l','L','1','i'],
    'L': ['l','L','1','i'],
    '1': ['l','L','1','i'],
    'i': ['l','L','1','i'],
    'o': ['o','0'],
    '0': ['o','0'],
    'm': ['m','M'],
    'M': ['m','M'],
    'k': ['k','K'],
    'K': ['k','K'],
    't': ['t','T'],
    'T': ['t','T']
}

# build a set of candidates by substituting ambiguous chars
candidates = set()
for base in base_candidates:
    # for each char, get options
    options = [amb_map.get(ch, [ch]) for ch in base]
    for combo in itertools.product(*options):
        candidates.add(''.join(combo))

print('Trying', len(candidates), 'passphrase candidates')

# function helpers
import hashlib

def md5hex(s): return hashlib.md5(s.encode('utf-8')).hexdigest()

def rfc3986(s): return urllib.parse.quote(s, safe='~').replace('+','%20')

# compute params dict for building bases
raw_params = params.copy()
# remove signature just in case
raw_params.pop('signature', None)

found = []
count = 0
for pf in candidates:
    for include_key in (True, False):
        for encode in (False, True):
            # append passphrase True only (initial request should have it)
            parts = []
            p = {k:v for k,v in raw_params.items() if v is not None and v != ''}
            if not include_key:
                p.pop('merchant_key', None)
            for k in sorted(p.keys()):
                v = p[k]
                val = rfc3986(v) if encode else v
                parts.append(f"{k}={val}")
            base = '&'.join(parts) + '&passphrase=' + pf
            sig = md5hex(base)
            count += 1
            if sig == provided:
                found.append((pf, include_key, encode, base, sig))
                print('MATCH FOUND! passphrase=',pf,' include_key=',include_key,' encode=',encode)
                print('BASE:', base)
                print('MD5 :', sig)
                break
        if found:
            break
    if found:
        break

print('Checked',count,'variants')
if not found:
    print('No match found among candidates')
else:
    print('Matches:', found)

