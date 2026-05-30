import json
import os

filepath = r'c:\Users\2303031050616\Desktop\rasaadarsh\app\src\main\assets\medicines.json'

with open(filepath, 'r', encoding='utf-8') as f:
    data = json.load(f)

new_data = []
for item in data:
    new_item = {
        'id': item.get('id'),
        'name': item.get('name'),
        'hindiName': item.get('hindiName'),
        'benefits': item.get('benefits'),
        'ingredients': item.get('ingredients'),
        'dose': item.get('dose'),
        'preparation': item.get('preparation'),
        'anupana': item.get('anupana'),
        'diseaseCategory': item.get('diseaseCategory'),
        'ingredientsListJson': item.get('ingredientsListJson', '[]')
    }
    new_data.append(new_item)

with open(filepath, 'w', encoding='utf-8') as f:
    json.dump(new_data, f, ensure_ascii=False, indent=2)

print(f"Cleaned {len(new_data)} items.")
