import json

def fix_encoding(text):
    if not isinstance(text, str):
        return text
    try:
        # Try to fix double encoding: UTF-8 -> Latin-1 -> UTF-8
        return text.encode('latin-1').decode('utf-8')
    except:
        return text

def process_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    for item in data:
        for key in item:
            if isinstance(item[key], str):
                item[key] = fix_encoding(item[key])
            elif isinstance(item[key], list):
                item[key] = [fix_encoding(i) for i in item[key]]
    
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=4)

if __name__ == "__main__":
    process_file(r'app\src\main\assets\medicines.json')
