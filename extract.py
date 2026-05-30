import json

with open('app/src/main/assets/medicines.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

for m in data:
    print(f"{m['id']}: {m['name']}")
