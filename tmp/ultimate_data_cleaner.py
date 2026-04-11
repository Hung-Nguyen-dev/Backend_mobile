import os
import json
import re

CITY_MAP = {
    "CaoBang": ["Cao Bằng"],
    "DaLat": ["Đà Lạt"],
    "DaNang": ["Đà Nẵng"],
    "HCM": ["Hồ Chí Minh", "Sài Gòn", "TPHCM", "TP. HCM"],
    "HaGiang": ["Hà Giang"],
    "HaNoi": ["Hà Nội"],
    "Hue": ["Huế"],
    "NhaTrang": ["Nha Trang"],
    "NinhBinh": ["Ninh Bình"],
    "PhuQuoc": ["Phú Quốc"],
    "QuangNinh": ["Quảng Ninh", "Hạ Long", "Cẩm Phả"],
    "ThanhHoa": ["Thanh Hóa"]
}

ALL_CITIES = [name for names in CITY_MAP.values() for name in names]

def clean_data(data, city_key):
    # Extract only local_results
    if isinstance(data, dict):
        results = data.get('local_results', [])
    elif isinstance(data, list):
        results = data
    else:
        return []

    cleaned = []
    seen_ids = set()
    seen_titles = set()

    for item in results:
        title = item.get('title', '').strip()
        address = item.get('address', '').strip()
        place_id = item.get('place_id', '')

        if not title or title == ".":
            continue

        # 1. City enforcement: Check if address belongs to a DIFFERENT city
        is_other_city = False
        for other_key, other_names in CITY_MAP.items():
            if other_key == city_key:
                continue
            
            for name in other_names:
                # regex to match whole word/phrase
                if re.search(r'\b' + re.escape(name) + r'\b', address, re.IGNORECASE):
                    is_other_city = True
                    break
            if is_other_city:
                break
        
        if is_other_city:
            # print(f"Removing {title} from {city_key} (found in address: {address})")
            continue

        # 2. De-duplication
        key = place_id if place_id else title.lower()
        if key in seen_ids or key in seen_titles:
            continue
        
        if place_id:
            seen_ids.add(place_id)
        seen_titles.add(title.lower())

        # 3. Simplify structure
        cleaned_item = {
            "title": title,
            "address": address,
            "rating": item.get('rating'),
            "reviews": item.get('reviews'),
            "type": item.get('type'),
            "thumbnail": item.get('thumbnail'),
            "thumbnail_large": item.get('thumbnail_large'),
            "gps_coordinates": item.get('gps_coordinates'),
            "hours": item.get('hours'),
            "price": item.get('price'),
            "description": item.get('description'),
            "place_id": place_id
        }
        cleaned.append(cleaned_item)

    return cleaned

def process_all_cities(base_path):
    for city_folder in os.listdir(base_path):
        if city_folder not in CITY_MAP:
            continue
            
        full_dir = os.path.join(base_path, city_folder)
        for filename in os.listdir(full_dir):
            if filename.endswith('.json'):
                file_path = os.path.join(full_dir, filename)
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        content = f.read().strip()
                        if not content:
                            continue
                            
                        # Handle concatenated JSON (like we found in Thanh Hoa)
                        if content.count('{"search_metadata"') > 1:
                            # Split into objects
                            blocks = re.split(r'(?={"search_metadata")', content)
                            content_data = []
                            for b in blocks:
                                if b.strip():
                                    try:
                                        content_data.append(json.loads(b))
                                    except: pass
                            # Flatten into a single results list
                            all_results = []
                            for d in content_data:
                                if isinstance(d, dict):
                                    all_results.extend(d.get('local_results', []))
                                elif isinstance(d, list):
                                    all_results.extend(d)
                            data = all_results
                        else:
                            data = json.loads(content)
                            
                        cleaned_results = clean_data(data, city_folder)
                        
                        # Write back as a simple list
                        with open(file_path, 'w', encoding='utf-8') as fw:
                            json.dump(cleaned_results, fw, ensure_ascii=False, indent=2)
                        
                        print(f"Cleaned {city_folder}/{filename}: {len(cleaned_results)} items.")
                except Exception as e:
                    print(f"Error processing {file_path}: {e}")

if __name__ == "__main__":
    process_all_cities("e:/Downloads/Backend_mobile-dev/Backend_mobile-dev/src/main/resources/city-data")
