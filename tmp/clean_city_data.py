import json
import os
import re

CITY_DATA_DIR = r"e:\Downloads\Backend_mobile-dev\Backend_mobile-dev\src\main\resources\city-data"

CITY_MAPPING = {
    "HaNoi": ["Hà Nội", "Hanoi"],
    "HCM": ["Hồ Chí Minh", "Sài Gòn", "Ho Chi Minh"],
    "DaNang": ["Đà Nẵng", "Da Nang"],
    "HaGiang": ["Hà Giang", "Ha Giang"],
    "NinhBinh": ["Ninh Bình", "Ninh Binh"],
    "QuangNinh": ["Quảng Ninh", "Quang Ninh", "Hạ Long", "Ha Long"],
    "ThanhHoa": ["Thanh Hóa", "Thanh Hoa"],
    "NhaTrang": ["Nha Trang", "Nha Trang"],
    "PhuQuoc": ["Phú Quốc", "Phu Quoc"],
    "Hue": ["Huế", "Hue"],
    "CaoBang": ["Cao Bằng", "Cao Bang"],
    "DaLat": ["Đà Lạt", "Da Lat"]
}

def parse_multi_json(content):
    """Parses a string containing multiple concatenated JSON objects."""
    results = []
    decoder = json.JSONDecoder()
    pos = 0
    while pos < len(content):
        content = content.lstrip()
        if not content:
            break
        try:
            obj, index = decoder.raw_decode(content)
            results.append(obj)
            content = content[index:]
        except json.JSONDecodeError:
            # Try to fix common issues like trailing commas or extra garbage
            break
    return results

def clean_file(file_path, city_key):
    if not os.path.exists(file_path):
        return

    with open(file_path, 'r', encoding='utf-8') as f:
        content = f.read()

    objects = parse_multi_json(content)
    if not objects:
        print(f"Skipping {file_path}: No valid JSON objects found.")
        return

    # Keep the latest metadata (assuming the last object is the most recent)
    latest_obj = objects[-1]
    
    all_results = []
    for obj in objects:
        if "local_results" in obj and isinstance(obj["local_results"], list):
            all_results.extend(obj["local_results"])

    # Deduplicate and Filter
    unique_results = []
    seen_ids = set()
    
    # Correct keywords for this city
    city_keywords = CITY_MAPPING.get(city_key, [])
    # Other cities' keywords to filter out
    other_cities = {k: v for k, v in CITY_MAPPING.items() if k != city_key}

    for res in all_results:
        place_id = res.get("place_id") or res.get("title", "") + str(res.get("gps_coordinates", ""))
        if place_id in seen_ids:
            continue
        
        # Filter by city
        address = res.get("address", "")
        title = res.get("title", "")
        
        is_wrong_city = False
        for other_key, keywords in other_cities.items():
            for kw in keywords:
                # If the address explicitly mentions another major city
                if f", {kw}" in address or address == kw:
                    is_wrong_city = True
                    break
            if is_wrong_city:
                break
        
        if is_wrong_city:
            # Special case: check if it ALSO mentions the current city
            is_right_city = False
            for kw in city_keywords:
                if kw in address or kw in title:
                    is_right_city = True
                    break
            if not is_right_city:
                continue # Discard if it only mentions another city

        unique_results.append(res)
        seen_ids.add(place_id)

    # Re-position
    for i, res in enumerate(unique_results):
        res["position"] = i + 1

    # Update the latest object with merged results
    latest_obj["local_results"] = unique_results
    
    # Remove pagination details as they might be invalid after merging
    if "pagination" in latest_obj:
        del latest_obj["pagination"]
    if "serpapi_pagination" in latest_obj:
        del latest_obj["serpapi_pagination"]

    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(latest_obj, f, ensure_ascii=False, indent=2)
    
    print(f"Cleaned {file_path}: {len(unique_results)} unique results.")

def main():
    for city_dir in os.listdir(CITY_DATA_DIR):
        dir_path = os.path.join(CITY_DATA_DIR, city_dir)
        if not os.path.isdir(dir_path):
            continue
        
        for file_name in ["tourism_places.json", "cafe.json", "restaurant.json"]:
            file_path = os.path.join(dir_path, file_name)
            clean_file(file_path, city_dir)

if __name__ == "__main__":
    main()
