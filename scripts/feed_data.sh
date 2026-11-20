#!/usr/bin/env bash
# SwiftBasket — feed catalog data as admin
# Usage: ./scripts/feed_data.sh
set -e

KONG=${KONG:-http://localhost:8000}
read -rp "Admin email: " EMAIL
read -rsp "Admin password: " PASSWORD; echo

echo "Logging in..."
LOGIN_JSON=$(python3 -c "import json,sys;print(json.dumps({'email':sys.argv[1],'password':sys.argv[2]}))" "$EMAIL" "$PASSWORD")
TOKEN=$(curl -sf -X POST "$KONG/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "$LOGIN_JSON" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['token'])")
export TOKEN

echo "Creating categories..."

curl -sf -X POST "$KONG/api/v1/categories" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name": "Mobiles", "description": "Smartphones, cases and screen guards"}' > /dev/null && echo "  category created: Mobiles" || echo "  (skipped: Mobiles — may already exist)"

curl -sf -X POST "$KONG/api/v1/categories" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name": "Home & Kitchen", "description": "Kitchen appliances, cookware and home essentials"}' > /dev/null && echo "  category created: Home & Kitchen" || echo "  (skipped: Home & Kitchen — may already exist)"

curl -sf -X POST "$KONG/api/v1/categories" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name": "Men's Fashion", "description": "Men's clothing, footwear and accessories"}' > /dev/null && echo "  category created: Men'\''s Fashion" || echo "  (skipped: Men'\''s Fashion — may already exist)"

curl -sf -X POST "$KONG/api/v1/categories" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name": "Women's Fashion", "description": "Women's clothing, ethnic wear and accessories"}' > /dev/null && echo "  category created: Women'\''s Fashion" || echo "  (skipped: Women'\''s Fashion — may already exist)"

curl -sf -X POST "$KONG/api/v1/categories" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name": "Sports & Fitness", "description": "Cricket, badminton, gym and outdoor gear"}' > /dev/null && echo "  category created: Sports & Fitness" || echo "  (skipped: Sports & Fitness — may already exist)"

curl -sf -X POST "$KONG/api/v1/categories" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name": "Books", "description": "Bestsellers, exam prep and reference"}' > /dev/null && echo "  category created: Books" || echo "  (skipped: Books — may already exist)"

curl -sf -X POST "$KONG/api/v1/categories" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"name": "Toys & Games", "description": "Board games, building sets and soft toys"}' > /dev/null && echo "  category created: Toys & Games" || echo "  (skipped: Toys & Games — may already exist)"

# resolve real category ids by name
CATEGORY_IDS=$(curl -sf "$KONG/api/v1/categories" | python3 -c '
import sys, json
cats = json.load(sys.stdin)
print(json.dumps({c["name"]: c["id"] for c in cats}))
')
export CATEGORY_IDS

echo "Creating products..."
python3 - <<"PYEOF"
import json, os, sys, urllib.request, urllib.error

kong = os.environ.get("KONG", "http://localhost:8000")
token = os.environ["TOKEN"]
cat_ids = json.loads(os.environ["CATEGORY_IDS"])
data = json.load(open(os.path.join(os.getcwd(), "docs", "seed_data.json")))

def api(method, path, body=None):
    req = urllib.request.Request(kong + path, method=method,
        data=json.dumps(body).encode() if body else None,
        headers={"Authorization": "Bearer " + token, "Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req) as r:
            return r.status, json.load(r)
    except urllib.error.HTTPError as e:
        return e.code, json.load(e)

ok = fail = skip = 0
for p in data["products"]:
    body = dict(p)
    # map categoryId 1..8 to the real id for that category name
    name_by_id = {
        1: "Electronics", 2: "Mobiles", 3: "Home & Kitchen", 4: "Men's Fashion",
        5: "Women's Fashion", 6: "Sports & Fitness", 7: "Books", 8: "Toys & Games"}
    real_id = cat_ids.get(name_by_id.get(body["categoryId"], ""), body["categoryId"])
    body["categoryId"] = real_id
    status, resp = api("POST", "/api/v1/products", body)
    if status == 201:
        ok += 1
    elif status == 409:
        skip += 1  # SKU already exists
    else:
        fail += 1
        print(f"  FAIL {body['sku']}: {status} {resp.get('message','')}")

print(f"products: {ok} created, {skip} already existed, {fail} failed")
PYEOF

echo "Rebuilding search index..."
COUNT=$(curl -sf -X POST "$KONG/api/v1/products/reindex" -H "Authorization: Bearer $TOKEN")
echo "Indexed $COUNT products."
echo "Done. Browse http://localhost:5173/shop"
