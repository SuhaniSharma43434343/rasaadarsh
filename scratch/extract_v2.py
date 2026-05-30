import re
import json
import os

def extract():
    file_path = 'app/src/main/java/com/example/rasaushadhies/ui/data/MedicineDatabase.kt'
    if not os.path.exists(file_path):
        print(f"Error: {file_path} not found")
        return

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # Regex to find all MedicineRecord constructor calls
    # We'll look for MedicineRecord(...) and handle nested parens
    records = []
    
    # Find all occurrences of "MedicineRecord("
    matches = list(re.finditer(r'MedicineRecord\s*\(', content))
    print(f"Found {len(matches)} potential MedicineRecord matches")
    
    for match in matches:
        start = match.start()
        # Find the matching closing parenthesis
        balance = 1
        end = -1
        i = match.end()
        while i < len(content):
            if content[i] == '(':
                balance += 1
            elif content[i] == ')':
                balance -= 1
                if balance == 0:
                    end = i
                    break
            i += 1
        
        if end != -1:
            block = content[start:end+1]
            record = {}
            
            # Extract id
            id_m = re.search(r'id\s*=\s*(\d+)', block)
            if id_m:
                record['id'] = int(id_m.group(1))
            else:
                continue # Skip if no ID
            
            # Extract string fields
            # Supports both "..." and """..."""
            string_fields = [
                'name', 'hindiName', 'benefits', 'ingredients', 'dose', 
                'preparation', 'properties', 'anupana', 'notes', 'etymology', 
                'tags', 'ingredientsListJson', 'benefitsListJson', 
                'preparationStepsJson', 'indicationsJson', 'propertiesStructuredJson'
            ]
            
            for field in string_fields:
                # Triple quotes
                tp = re.search(fr'{field}\s*=\s*"""(.*?)"""', block, re.DOTALL)
                if tp:
                    record[field] = tp.group(1).strip()
                else:
                    # Normal quotes - handle escaped quotes
                    # This regex is better: field = " ( [^"\\] | \\. )* "
                    nm = re.search(fr'{field}\s*=\s*"((?:[^"\\]|\\.)*)"', block, re.DOTALL)
                    if nm:
                        val = nm.group(1)
                        # Unescape
                        val = val.replace('\\n', '\n').replace('\\t', '\t').replace('\\"', '"').replace('\\\\', '\\')
                        record[field] = val
                    else:
                        record[field] = ""
            
            # Extract boolean fields
            bool_fields = ['isHighAlert', 'containsHeavyMetals', 'containsPoison']
            for bf in bool_fields:
                bm = re.search(fr'{bf}\s*=\s*(true|false)', block)
                if bm:
                    record[bf] = bm.group(1) == 'true'
                else:
                    record[bf] = False
            
            records.append(record)

    # Deduplicate and sort
    unique_records = {r['id']: r for r in records}.values()
    sorted_records = sorted(unique_records, key=lambda x: x['id'])

    # Final check on some samples
    if sorted_records:
        r1 = sorted_records[0]
        print(f"Sample Record 1: {r1['name']}, Benefits Length: {len(r1['benefits'])}")
        if len(r1['benefits']) < 10:
            print("WARNING: Benefits for Record 1 seems too short!")

    # Write to file
    output_path = 'app/src/main/assets/medicines.json'
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(sorted_records, f, indent=2, ensure_ascii=False)
    
    print(f"Extraction complete. Extracted {len(sorted_records)} unique records.")

if __name__ == '__main__':
    extract()
