import json
import re

with open('app/src/main/assets/medicines.json', 'r', encoding='utf-8') as f:
    data = json.load(f)

diseases = [
    "Aamvata",
    "Agnimandya, Ajirna, Visuchika",
    "Amlapita",
    "Arsha",
    "Atisara",
    "Bhagandar",
    "Grahani",
    "Gulma",
    "Hriday Roga",
    "Jwara",
    "Kasa",
    "Kushtha",
    "Mutrakruccha, Mutraghata",
    "Netra Roga",
    "Pandu-Kamala",
    "Pliha Roga",
    "Prameha",
    "Rajayakshama",
    "Raktapitta",
    "Shiro Roga",
    "Shwas Hikka",
    "Stri Roga",
    "Udar Roga",
    "Unmad Apasmar",
    "Vata vyadhi"
]

def find_disease(medicine):
    text = (medicine.get('tags', '') + ' ' + medicine.get('benefits', '') + ' ' + medicine.get('name', '')).lower()
    
    # Custom matches based on synonyms
    if 'rheumatoid' in text or 'aamvata' in text or 'amavata' in text: return "Aamvata"
    if 'indigestion' in text or 'agnimandya' in text or 'ajirna' in text or 'visuchika' in text: return "Agnimandya, Ajirna, Visuchika"
    if 'acidity' in text or 'amlapita' in text or 'amlapitta' in text: return "Amlapita"
    if 'piles' in text or 'arsha' in text or 'hemorrhoid' in text: return "Arsha"
    if 'diarrhea' in text or 'atisara' in text: return "Atisara"
    if 'fistula' in text or 'bhagandar' in text: return "Bhagandar"
    if 'ibs' in text or 'grahani' in text: return "Grahani"
    if 'tumor' in text or 'gulma' in text: return "Gulma"
    if 'heart' in text or 'hriday' in text or 'hridya' in text: return "Hriday Roga"
    if 'fever' in text or 'jwara' in text: return "Jwara"
    if 'cough' in text or 'kasa' in text: return "Kasa"
    if 'skin' in text or 'kushtha' in text or 'leprosy' in text: return "Kushtha"
    if 'dysuria' in text or 'mutra' in text: return "Mutrakruccha, Mutraghata"
    if 'eye' in text or 'netra' in text: return "Netra Roga"
    if 'anemia' in text or 'pandu' in text or 'kamala' in text or 'jaundice' in text: return "Pandu-Kamala"
    if 'spleen' in text or 'pliha' in text: return "Pliha Roga"
    if 'diabetes' in text or 'prameha' in text: return "Prameha"
    if 'tuberculosis' in text or 'rajayakshama' in text: return "Rajayakshama"
    if 'bleeding' in text or 'raktapitta' in text: return "Raktapitta"
    if 'head' in text or 'shiro' in text: return "Shiro Roga"
    if 'asthma' in text or 'shwas' in text or 'hikka' in text: return "Shwas Hikka"
    if 'gynaecological' in text or 'stri' in text or 'menstrual' in text: return "Stri Roga"
    if 'abdominal' in text or 'udar' in text: return "Udar Roga"
    if 'insanity' in text or 'epilepsy' in text or 'unmad' in text or 'apasmar' in text: return "Unmad Apasmar"
    if 'vata' in text or 'neurological' in text: return "Vata vyadhi"
    
    return None

last_disease = "Aamvata"
for m in data:
    d = find_disease(m)
    if d is not None:
        last_disease = d
    m['diseaseCategory'] = last_disease

with open('app/src/main/assets/medicines.json', 'w', encoding='utf-8') as f:
    json.dump(data, f, ensure_ascii=False, indent=2)

print("Successfully categorized medicines.")
