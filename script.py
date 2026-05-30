import json
import re

with open('app/src/androidTest/raushadies.txt', 'r', encoding='utf-8') as f:
    content = f.read()

# Try to parse the content. It contains multiple JSON objects separated by newlines or commas
content = re.sub(r'}\s*{', '},{', content)
if not content.strip().startswith('['):
    content = f'[{content}]'

try:
    data = json.loads(content)
except Exception as e:
    print('JSON Parse Error:', e)
    import sys
    sys.exit(1)

def extract_string(val):
    if not val:
        return ""
    if isinstance(val, str):
        return val
    if isinstance(val, list):
        return ", ".join([extract_string(v) for v in val])
    if isinstance(val, dict):
        parts = []
        for k, v in val.items():
            parts.append(f"{k.capitalize()}: {extract_string(v)}")
        return ", ".join(parts)
    return str(val)

records = []
for item in data:
    name = item.get('name', '')
    benefits = extract_string(item.get('benefits', ''))
    
    # ingredients usually an array of objects
    ing_raw = item.get('ingredients', [])
    if isinstance(ing_raw, list):
        ings = []
        for d in ing_raw:
            if isinstance(d, dict):
                en = d.get('english', '')
                if en:
                    ings.append(en)
                else:
                    ings.append(d.get('name_sanskrit', d.get('sanskrit', '')))
            else:
                ings.append(str(d))
        ingredients = ', '.join(ings)
    else:
        ingredients = extract_string(ing_raw)
        
    dose = extract_string(item.get('dose', ''))
    if dose == "[object Object]": # if it somehow was hardcoded
        dose = ""
    
    final_prep = extract_string(item.get('preparation', '')).strip()
    processing = extract_string(item.get('processing', '')).strip()
    
    if not final_prep and processing:
        final_prep = processing
    if not final_prep:
        final_prep = 'Preparation not available'
        
    properties = extract_string(item.get('properties', ''))
    
    anupana = extract_string(item.get('anupana', ''))
    if not anupana:
        anupana = extract_string(item.get('anupana_condition_based', ''))
        
    # Escape quotes and clean newlines
    name = name.replace('"', '\\"').replace('\n', ' ')
    benefits = benefits.replace('"', '\\"').replace('\n', ' ')
    ingredients = ingredients.replace('"', '\\"').replace('\n', ' ')
    dose = dose.replace('"', '\\"').replace('\n', ' ')
    final_prep = final_prep.replace('"', '\\"').replace('\n', ' ')
    properties = properties.replace('"', '\\"').replace('\n', ' ')
    anupana = anupana.replace('"', '\\"').replace('\n', ' ')
    
    records.append(f'        MedicineRecord("{name}","{benefits}","{ingredients}","{dose}","{final_prep}","{properties}","{anupana}")')

print(f'Total records parsed: {len(records)}')

new_content = '    val ALL: List<MedicineRecord> = listOf(\n' + ',\n'.join(records) + '\n    )'

with open('app/src/main/java/com/example/rasaushadhies/ui/data/MedicineDatabase.kt', 'r', encoding='utf-8') as f:
    db_content = f.read()

# Replace val ALL... up to the end of the listOf()
new_db_content = re.sub(
    r'val ALL: List<MedicineRecord> = listOf\(.*?\n    \)', 
    new_content.replace('\\\\', '\\'), 
    db_content, 
    flags=re.DOTALL
)

with open('app/src/main/java/com/example/rasaushadhies/ui/data/MedicineDatabase.kt', 'w', encoding='utf-8') as f:
    f.write(new_db_content)

print('Updated MedicineDatabase.kt')

