const fs = require('fs');
const path = require('path');

const RAW_DATA_DIR = './raw_data';
const BATCH_SIZE = 50;

// EXTREMELY STRICT TITLE REGEX
const TITLE_REGEX = /^(\d{1,3})\.\s+([A-Z][^(\n]+?)\s*\(([\u0900-\u097F][^)]+)\)/;

// Exclusion list for false positives
const EXCLUDE_WORDS = ["Take", "Dissolve", "Mix", "Form", "Consumption", "Ingredients", "Give", "Place"];

function isRealTitle(line) {
    const match = line.match(TITLE_REGEX);
    if (!match) return false;
    const englishName = match[2].trim();
    if (EXCLUDE_WORDS.some(word => englishName.startsWith(word))) return false;
    return true;
}

function isShloka(line) {
    if (!line || line.length < 5) return false;
    const lower = line.toLowerCase();
    const UNIT_KEYWORDS = ["gm", "mg", "part", "tola", "masha", "ratti", "tablet", "decoc", "powder", "quantity", "ingredients"];
    const hasQuantity = /\d+\s*(gm|mg|part|tola|masha|ratti|tablet)/i.test(line);
    if (UNIT_KEYWORDS.some(unit => lower.includes(unit)) || hasQuantity) return false;
    if (/[\u0900-\u097F]/.test(line)) return true;
    if (line.includes('||')) return true;
    const words = line.split(/\s+/);
    if (words.length > 5) {
        const hasCommon = lower.includes('the ') || lower.includes(' and ') || lower.includes(' is ') || lower.includes(' for ') || lower.includes(' with ');
        if (!hasCommon) return true;
    }
    return false;
}

const DISEASE_MAP = {
    "Arsha": ["Hemorrhoids", "Piles", "Rectal Disorders"],
    "Bhagandara": ["Fistula-in-ano", "Anal Fistula", "Rectal Disorders"],
    "Gudamaya": ["Rectal Disorders", "Anal Diseases"],
    "Pandu": ["Anemia"],
    "Kamala": ["Jaundice"],
    "Amlapitta": ["Acidity", "Gastritis"],
    "Grahani": ["IBS", "Malabsorption", "Digestive Disorders"],
    "Kasa": ["Cough"],
    "Shvasa": ["Asthma", "Respiratory Disorders"],
    "Prameha": ["Diabetes", "Urinary Disorders"],
    "Kushtha": ["Skin Diseases", "Dermatosis"],
    "Jwara": ["Fever"],
    "Vataroga": ["Neurological Disorders", "Joint Pain"],
    "Amavata": ["Rheumatoid Arthritis"],
    "Sandhivata": ["Osteoarthritis"],
    "Hridroga": ["Cardiac Disorders"],
    "Mutrakricchra": ["Dysuria", "Urinary Infection"],
    "Shula": ["Abdominal Pain", "Colic"],
    "Gulma": ["Abdominal Tumor", "Bloating"]
};

function extractDoseFromText(text) {
    if (!text) return null;
    // Regex for specific Ayurvedic doses: numbers followed by Ratti, Gunja, mg, tablet, etc.
    const doseRegex = /(\d+(?:\.\d+)?\s*(?:Ratti|Gunja|mg|tablet|pill|gm|grain|tola|masha|ratti))/i;
    const match = text.match(doseRegex);
    return match ? match[1].trim() : null;
}

function extractTags(text, indications = "") {
    let tags = new Set();
    const combinedText = (text + " " + indications).toLowerCase();
    
    // 1. Map Sanskrit diseases to English
    for (let [sanskrit, englishList] of Object.entries(DISEASE_MAP)) {
        if (combinedText.includes(sanskrit.toLowerCase())) {
            englishList.forEach(e => tags.add(e));
        }
    }
    
    // 2. Extract content inside brackets e.g. "Arsha (haemorrhoids)"
    const bracketRegex = /\(([^)]+)\)/g;
    let bracketMatch;
    while ((bracketMatch = bracketRegex.exec(combinedText)) !== null) {
        const inner = bracketMatch[1].trim();
        // Only add if it's not a reference like (125 mg)
        if (!/\d/.test(inner) && inner.length > 3) {
            // Capitalize first letter
            tags.add(inner.charAt(0).toUpperCase() + inner.slice(1));
        }
    }

    // 3. Fallback to existing metadata tags if any
    const COMMON_KEYWORDS = ["Bleeding", "Digestion", "Cough", "Asthma", "Diabetes", "Skin", "Cardiac", "Fever"];
    COMMON_KEYWORDS.forEach(kw => {
        if (combinedText.includes(kw.toLowerCase())) tags.add(kw);
    });

    return Array.from(tags).filter(t => t.length > 2);
}

function extractSection(lines, startIndex, headerKeywords, stopKeywords = [], filterShlokas = true) {
    let content = [];
    let foundHeader = false;
    let headerToMatch = headerKeywords.map(k => k.toLowerCase());
    const stopToMatch = stopKeywords.map(k => k.toLowerCase());
    
    for (let i = startIndex; i < lines.length; i++) {
        const line = lines[i].trim();
        const lowerLine = line.toLowerCase();
        if (i > startIndex && isRealTitle(line)) break;
        if (!foundHeader) {
            if (headerToMatch.some(k => lowerLine.includes(k))) {
                foundHeader = true;
                continue;
            }
        } else {
            const isStop = stopToMatch.some(sk => lowerLine.startsWith(sk));
            if (isStop || (line.match(/^\d{1,3}\./) && isRealTitle(line))) break;
            if (line) {
                if (filterShlokas && isShloka(line)) continue;
                content.push(line);
            }
        }
    }
    return content.join('\\n').trim();
}

function parseIngredientsList(rawText) {
    const lines = rawText.split('\\n');
    let ingredients = [];
    lines.forEach(line => {
        if (line.toLowerCase().includes("ingredient") && line.toLowerCase().includes("quantity")) return;
        if (line.toLowerCase().startsWith("shloka")) return;
        
        let cells = line.split(/\t| {2,}/).map(c => c.trim()).filter(c => c.length > 0);
        if (cells.length >= 3) {
            ingredients.push({ sanskritName: cells[0], englishName: cells[1], quantity: cells[2] });
        } else if (cells.length === 2) {
            ingredients.push({ sanskritName: cells[0], englishName: cells[1], quantity: "-" });
        } else if (line.length > 2) {
            ingredients.push({ sanskritName: line, englishName: "-", quantity: "-" });
        }
    });
    return ingredients;
}

function parseProperties(rawText, fallbacks = "") {
    const props = { dose: "", taste: "", smell: "", color: "", virya: "", vipaka: "", anupana: "", indication: "" };
    // Replace literal \t or double spaces with a standard separator to handle table-like structures
    const combined = (rawText + " " + fallbacks).replace(/\\t/g, "  ");
    
    const patterns = {
        dose: [/Dose \(Matra\)\s+([^\n\\]+)/i, /Dosage\s*:\s*([^\n\\]+)/i],
        taste: [/Taste \(Svad\)\s+([^\n\\]+)/i, /Taste\s*:\s*([^\n\\]+)/i, /Svada\s*:\s*([^\n\\]+)/i],
        smell: [/Smell \(Gandha\)\s+([^\n\\]+)/i, /Smell\s*:\s*([^\n\\]+)/i],
        color: [/Color \(Varna\)\s+([^\n\\]+)/i, /Color\s*:\s*([^\n\\]+)/i],
        virya: [/Virya\s+([^\n\\]+)/i, /Potency\s*:\s*([^\n\\]+)/i],
        vipaka: [/Vipaka\s+([^\n\\]+)/i, /Post-Digestion\s*:\s*([^\n\\]+)/i],
        anupana: [/Vehicle \(Anupana\)\s+([^\n\\]+)/i, /Anupana\s*:\s*([^\n\\]+)/i],
        indication: [/Indication \(Upayoga\)\s+([^\n\\]+)/i, /Indicated for\s*:\s*([^\n\\]+)/i]
    };

    for (let [key, regexes] of Object.entries(patterns)) {
        for (let regex of regexes) {
            const match = combined.match(regex);
            if (match) {
                props[key] = match[1].trim();
                break;
            }
        }
    }
    
    return props;
}

function detectSafety(ingredients, preparation, properties) {
    const combined = (ingredients + " " + preparation + " " + properties).toLowerCase();
    
    const heavyMetalKeywords = ["parada", "mercury", "tamra", "copper", "loha", "iron", "naga", "lead", "vanga", "tin", "abhraka", "mica", "haritala", "orpiment", "manashila", "realgar"];
    const poisonKeywords = ["vatsanabha", "aconite", "vish", "poison", "tox", "dhattura", "datura", "kupilu", "jayaphala"];
    
    const containsHeavyMetals = heavyMetalKeywords.some(kw => combined.includes(kw));
    const containsPoison = poisonKeywords.some(kw => combined.includes(kw));
    const isHighAlert = containsHeavyMetals || containsPoison || combined.includes("caution") || combined.includes("avoid");
    
    return { containsHeavyMetals, containsPoison, isHighAlert };
}

function parseFile(content) {
    const lines = content.split('\n');
    const medicines = [];
    
    for (let i = 0; i < lines.length; i++) {
        const line = lines[i].trim();
        if (isRealTitle(line)) {
            const match = line.match(TITLE_REGEX);
            const medId = parseInt(match[1]);
            
            const ALL_HEADERS = ["Therapeutic Benefits", "Indication", "Indicated for", "Therapeutic Range", "Ingredients", "Method of Preparation", "Vidhi", "Dosage", "Dose", "Properties", "Vehicle", "Caution", "Note", "Reference", "Sanskrit Shloka"];

            let rawBenefits = extractSection(lines, i, ["Therapeutic Benefits", "Indication", "Indicated for", "Therapeutic Range"], ALL_HEADERS.filter(h => !["Therapeutic Benefits", "Indication", "Indicated for", "Therapeutic Range"].includes(h)), true);
            let rawIngredients = extractSection(lines, i, ["Ingredients (Ghatak)", "Ingredients", "Ingredients (Dravya)", "Composition"], ALL_HEADERS.filter(h => !["Ingredients"].includes(h)), true);
            let rawPreparation = extractSection(lines, i, ["Method of Preparation", "Vidhi"], ALL_HEADERS.filter(h => !["Method of Preparation", "Vidhi"].includes(h)), true);
            let rawDose = extractSection(lines, i, ["Dosage", "Dose (Matra)", "Dosage & Administration"], ALL_HEADERS.filter(h => !["Dosage", "Dose"].includes(h)), true);
            let rawProperties = extractSection(lines, i, ["Properties"], ALL_HEADERS.filter(h => !["Properties"].includes(h)), false); 
            
            // Fix anupana extraction to not be too greedy with property tables
            let rawAnupana = extractSection(lines, i, ["Vehicle (Anupana)"], ALL_HEADERS.filter(h => !["Vehicle"].includes(h)), true);
            let notes = extractSection(lines, i, ["Notes", "Caution", "Note:"], ["Sanskrit Shloka"], true);
            
            // Intelligent Data Cleaning & Fallback Logic
            const ingredientsList = parseIngredientsList(rawIngredients);
            
            // SWEEP: Multi-section property analysis
            const structuredProps = parseProperties(rawProperties, rawBenefits + " " + notes + " " + rawAnupana);
            
            // DOSE PRIORITY: 1. Table Dose, 2. Text Dose, 3. Section Dose
            let dose = structuredProps.dose || extractDoseFromText(rawBenefits) || extractDoseFromText(rawPreparation) || rawDose || "Consult Physician";
            
            // ANUPANA PRIORITY: 1. Main Section, 2. Table Anupana
            let anupana = rawAnupana;
            if ((!anupana || anupana.includes("Taste (Svad)")) && structuredProps.anupana) {
                anupana = structuredProps.anupana;
            }
            
            const benefitsList = rawBenefits.split(/\\n|\. |; /).map(s => s.trim()).filter(s => s.length > 5);
            const preparationSteps = rawPreparation.split(/\\n|\d+\. /).map(s => s.trim()).filter(s => s.length > 5);
            
            const safety = detectSafety(rawIngredients, rawPreparation, rawProperties);
            let finalIndications = extractTags(rawBenefits, rawProperties + " " + (structuredProps.indication || ""));
            
            medicines.push({
                id: medId,
                name: match[2].trim(),
                hindiName: match[3].trim(),
                benefits: rawBenefits || "No description available.",
                benefitsList: benefitsList,
                ingredients: rawIngredients,
                ingredientsList: ingredientsList,
                preparation: rawPreparation,
                preparationSteps: preparationSteps,
                dose: dose,
                anupana: anupana,
                properties: rawProperties,
                propertiesStructured: structuredProps,
                notes: notes,
                tags: finalIndications.join(","),
                indications: finalIndications,
                ...safety
            });
        }
    }
    return medicines;
}

let allMedicines = [];
const files = fs.readdirSync(RAW_DATA_DIR).filter(f => f.endsWith('.txt')).sort((a,b) => parseInt(a.match(/\d+/)[0]) - parseInt(b.match(/\d+/)[0]));
files.forEach(file => {
    const content = fs.readFileSync(path.join(RAW_DATA_DIR, file), 'utf8');
    allMedicines = allMedicines.concat(parseFile(content));
});

const uniqueMeds = [];
const seenIds = new Set();
allMedicines.sort((a,b) => a.id - b.id).forEach(m => {
    if (!seenIds.has(m.id)) { uniqueMeds.push(m); seenIds.add(m.id); }
});

function escape(str) {
    if (!str) return "";
    return str.replace(/\\/g, '\\\\').replace(/"/g, '\\"').replace(/\n/g, ' ').replace(/\r/g, ' ');
}

// Generate Kotlin records
let output = `package com.example.rasaushadhies.ui.data

import com.example.rasaushadhies.data.local.Ingredient
import com.example.rasaushadhies.data.local.ClinicalProperties
import com.google.gson.Gson
import com.example.rasaushadhies.data.local.MedicineTypeConverters

data class MedicineRecord(
    val id: Int,
    val name: String,
    val hindiName: String,
    val benefits: String,
    val ingredients: String,
    val dose: String,
    val preparation: String,
    val properties: String,
    val anupana: String,
    val notes: String = "",
    val etymology: String = "",
    val tags: String = "",
    val isHighAlert: Boolean = false,
    val containsHeavyMetals: Boolean = false,
    val containsPoison: Boolean = false,
    
    // Structured JSON Fields for Seeding
    val ingredientsListJson: String = "[]",
    val benefitsListJson: String = "[]",
    val preparationStepsJson: String = "[]",
    val indicationsJson: String = "[]",
    val propertiesStructuredJson: String = "{}"
)

object MedicineDatabase {
    private val gson = Gson()
    
    fun search(query: String, limit: Int = 10): List<MedicineRecord> {
        val q = query.lowercase().trim()
        if (q.isEmpty()) return emptyList()
        return ALL.filter { 
            it.name.lowercase().contains(q) || 
            it.hindiName.lowercase().contains(q) ||
            it.benefits.lowercase().contains(q) ||
            it.tags.lowercase().contains(q)
        }.take(limit)
    }
`;

const batches = [];
for (let i = 0; i < uniqueMeds.length; i += BATCH_SIZE) {
    batches.push(uniqueMeds.slice(i, i + BATCH_SIZE));
}

batches.forEach((batch, i) => {
    output += `\n    private val batch${i + 1} = listOf(\n`;
    batch.forEach(m => {
        output += `            MedicineRecord(
                id = ${m.id},
                name = "${escape(m.name)}",
                hindiName = "${escape(m.hindiName)}",
                benefits = "${escape(m.benefits)}",
                ingredients = "${escape(m.ingredients)}",
                dose = "${escape(m.dose)}",
                preparation = "${escape(m.preparation)}",
                properties = "${escape(m.properties)}",
                anupana = "${escape(m.anupana)}",
                notes = "${escape(m.notes)}",
                tags = "${escape(m.tags)}",
                isHighAlert = ${m.isHighAlert},
                containsHeavyMetals = ${m.containsHeavyMetals},
                containsPoison = ${m.containsPoison},
                ingredientsListJson = "${escape(JSON.stringify(m.ingredientsList))}",
                benefitsListJson = "${escape(JSON.stringify(m.benefitsList))}",
                preparationStepsJson = "${escape(JSON.stringify(m.preparationSteps))}",
                indicationsJson = "${escape(JSON.stringify(m.indications))}",
                propertiesStructuredJson = "${escape(JSON.stringify(m.propertiesStructured))}"
            ),\n`;
    });
    output += `    )\n`;
});

output += `\n    val ALL = ${batches.map((_, i) => `batch${i + 1}`).join(' + ')}\n`;
output += `}\n`;

fs.writeFileSync('./app/src/main/java/com/example/rasaushadhies/ui/data/MedicineDatabase.kt', output);
console.log(`Parsed ${uniqueMeds.length} medicines with Advanced Extraction. Database Updated.`);
