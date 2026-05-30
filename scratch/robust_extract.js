const fs = require('fs');

function extract() {
    const filePath = 'app/src/main/java/com/example/rasaushadhies/ui/data/MedicineDatabase.kt';
    if (!fs.existsSync(filePath)) {
        console.error('File not found:', filePath);
        return;
    }

    const content = fs.readFileSync(filePath, 'utf-8');
    const records = [];
    
    let lastIndex = 0;
    while (true) {
        let startIndex = content.indexOf('MedicineRecord(', lastIndex);
        if (startIndex === -1) break;
        
        // Find matching closing parenthesis
        let depth = 0;
        let endIndex = -1;
        for (let i = startIndex + 'MedicineRecord('.length; i < content.length; i++) {
            if (content[i] === '(') depth++;
            else if (content[i] === ')') {
                if (depth === 0) {
                    endIndex = i;
                    break;
                } else {
                    depth--;
                }
            }
        }
        
        if (endIndex === -1) break;
        
        const block = content.substring(startIndex, endIndex + 1);
        const record = extractFields(block);
        if (record && record.id) {
            records.push(record);
        }
        lastIndex = endIndex + 1;
    }

    // Deduplicate and sort
    const unique = Array.from(new Map(records.map(r => [r.id, r])).values());
    unique.sort((a, b) => a.id - b.id);

    console.log(`Extracted ${unique.length} records`);
    fs.writeFileSync('app/src/main/assets/medicines.json', JSON.stringify(unique, null, 2));
}

function extractFields(block) {
    const record = {};
    
    // Extract ID
    const idMatch = block.match(/id\s*=\s*(\d+)/);
    if (!idMatch) return null;
    record.id = parseInt(idMatch[1]);

    const fields = [
        'name', 'hindiName', 'benefits', 'ingredients', 'dose', 
        'preparation', 'properties', 'anupana', 'notes', 'etymology', 
        'tags', 'ingredientsListJson', 'benefitsListJson', 
        'preparationStepsJson', 'indicationsJson', 'propertiesStructuredJson'
    ];

    fields.forEach(field => {
        // Try triple quotes
        let startToken = `${field} = """`;
        let startIdx = block.indexOf(startToken);
        if (startIdx !== -1) {
            let valStart = startIdx + startToken.length;
            let valEnd = block.indexOf('"""', valStart);
            if (valEnd !== -1) {
                record[field] = block.substring(valStart, valEnd).trim();
                return;
            }
        }

        // Try single quotes
        startToken = `${field} = "`;
        startIdx = block.indexOf(startToken);
        if (startIdx !== -1) {
            let valStart = startIdx + startToken.length;
            // Find end quote, handling escapes
            let val = "";
            let i = valStart;
            while (i < block.length) {
                if (block[i] === '"') {
                    break;
                } else if (block[i] === '\\') {
                    i++;
                    if (block[i] === 'n') val += '\n';
                    else if (block[i] === 't') val += '\t';
                    else if (block[i] === '"') val += '"';
                    else if (block[i] === '\\') val += '\\';
                    else val += block[i];
                } else {
                    val += block[i];
                }
                i++;
            }
            record[field] = val;
            return;
        }

        // Default
        if (field === 'ingredientsListJson' || field === 'benefitsListJson' || field === 'preparationStepsJson' || field === 'indicationsJson') {
            record[field] = "[]";
        } else if (field === 'propertiesStructuredJson') {
            record[field] = "{}";
        } else {
            record[field] = "";
        }
    });

    // Booleans
    ['isHighAlert', 'containsHeavyMetals', 'containsPoison'].forEach(field => {
        const match = new RegExp(`${field}\\s*=\\s*(true|false)`).exec(block);
        record[field] = match ? match[1] === 'true' : false;
    });

    return record;
}

extract();
