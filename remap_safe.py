import json
import os

def get_category(medicine_id):
    if medicine_id <= 10: return "Aamvata"
    if medicine_id <= 20: return "Agnimandya, Ajirna, Visuchika"
    if medicine_id <= 29: return "Amlapita"
    if medicine_id <= 40: return "Arsha"
    if medicine_id <= 47: return "Atisara"
    if medicine_id <= 51: return "Bhagandar"
    if medicine_id <= 65: return "Grahani"
    if medicine_id <= 72: return "Gulma"
    if medicine_id <= 82: return "Hriday Roga"
    if medicine_id <= 100: return "Jwara"
    if medicine_id <= 112: return "Kasa"
    if medicine_id <= 125: return "Kushtha"
    if medicine_id <= 135: return "Mutrakruccha, Mutraghata"
    if medicine_id <= 140: return "Netra Roga"
    if medicine_id <= 150: return "Pandu-Kamala"
    if medicine_id <= 155: return "Pliha Roga"
    if medicine_id <= 170: return "Prameha"
    if medicine_id <= 180: return "Rajayakshama"
    if medicine_id <= 190: return "Raktapitta"
    if medicine_id <= 195: return "Shiro Roga"
    if medicine_id <= 210: return "Shwas Hikka"
    if medicine_id <= 225: return "Stri Roga"
    if medicine_id <= 235: return "Udar Roga"
    if medicine_id <= 240: return "Unmad Apasmar"
    return "Vata vyadhi"

def remap_json(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    for item in data:
        item['diseaseCategory'] = get_category(item['id'])
    
    with open(filepath, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=4)

if __name__ == "__main__":
    remap_json(r'app\src\main\assets\medicines.json')
