import json
import os
import re

def normalize_text(text):
    if not text:
        return ""
    # Remove diacritics/accents (simplified for Python)
    # In a real scenario, use unidecode, but here we can just lowercase and strip
    return re.sub(r'\s+', ' ', text.lower().strip())

def process_city_file(file_path):
    print(f"Processing {file_path}...")
    if not os.path.exists(file_path):
        print(f"File not found: {file_path}")
        return

    content = ""
    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    # The file contains multiple concatenated JSON objects: { ... } { ... }
    # We need to extract all 'local_results' from these objects.
    
    # Simple regex to find top-level objects might be tricky if nested.
    # But since it's SerpApi output, they are likely separated by newlines or just } {
    
    decoder = json.JSONDecoder()
    pos = 0
    all_results = []
    
    while pos < len(content.strip()):
        try:
            obj, pos = decoder.raw_decode(content, pos)
            if 'local_results' in obj and isinstance(obj['local_results'], list):
                all_results.extend(obj['local_results'])
            # Skip whitespace
            while pos < len(content) and content[pos].isspace():
                pos += 1
        except json.JSONDecodeError as e:
            print(f"Error decoding JSON at position {pos}: {e}")
            break

    if not all_results:
        print("No local_results found.")
        return

    # Deduplicate
    unique_places = {}
    for place in all_results:
        title = place.get('title', '')
        # Normalizing title and address for key
        address = place.get('address', '')
        key = normalize_text(title) + "|" + normalize_text(address)
        
        if key not in unique_places:
            unique_places[key] = place
        else:
            # Keep the one with higher rating or more reviews
            existing = unique_places[key]
            existing_rating = existing.get('rating', 0)
            existing_reviews = existing.get('reviews', 0)
            new_rating = place.get('rating', 0)
            new_reviews = place.get('reviews', 0)
            
            if new_reviews > existing_reviews or (new_reviews == existing_reviews and new_rating > existing_rating):
                unique_places[key] = place

    deduplicated_results = list(unique_places.values())
    print(f"Original items: {len(all_results)}, Unique items: {len(deduplicated_results)}")

    # Construct final object
    final_obj = {
        "local_results": deduplicated_results
    }

    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(final_obj, f, ensure_ascii=False, indent=2)

files_to_fix = [
    r"e:\Downloads\Backend_mobile-dev\Backend_mobile-dev\src\main\resources\city-data\QuangNinh\restaurant.json",
    r"e:\Downloads\Backend_mobile-dev\Backend_mobile-dev\src\main\resources\city-data\QuangNinh\tourism_places.json",
    r"e:\Downloads\Backend_mobile-dev\Backend_mobile-dev\src\main\resources\city-data\ThanhHoa\cafe.json",
    r"e:\Downloads\Backend_mobile-dev\Backend_mobile-dev\src\main\resources\city-data\ThanhHoa\restaurant.json"
]

for f in files_to_fix:
    process_city_file(f)
