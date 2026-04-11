import os
import json
import re
from difflib import SequenceMatcher

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

def normalize(s):
    if not s: return ""
    # remove diacritics
    s = s.lower()
    s = re.sub(r'[àáạảãâầấậẩẫăằắặẳẵ]', 'a', s)
    s = re.sub(r'[èéẹẻẽêềếệểễ]', 'e', s)
    s = re.sub(r'[ìíịỉĩ]', 'i', s)
    s = re.sub(r'[òóọỏõôồốộổỗơờớợởỡ]', 'o', s)
    s = re.sub(r'[ùúụủũưừứựửữ]', 'u', s)
    s = re.sub(r'[ỳýỵỷỹ]', 'y', s)
    s = re.sub(r'đ', 'd', s)
    # remove non-alphanumeric
    s = re.sub(r'[^a-z0-9]', '', s)
    return s

def similarity(a, b):
    return SequenceMatcher(None, a, b).ratio()

def process_city(base_path, city_folder):
    full_dir = os.path.join(base_path, city_folder)
    all_items = []
    
    # Load all files
    files = ["tourism_places.json", "cafe.json", "restaurant.json"]
    for f_name in files:
        f_path = os.path.join(full_dir, f_name)
        if os.path.exists(f_path):
            with open(f_path, 'r', encoding='utf-8') as f:
                try:
                    data = json.load(f)
                    for item in data:
                        item['_origin_file'] = f_name
                        all_items.append(item)
                except: pass

    if not all_items: return

    unique_items = []
    seen_images = {} # thumb_url -> title

    # Sort by reviews descending to keep the most "proven" record
    def get_reviews(x):
        r = x.get('reviews')
        if isinstance(r, (int, float)): return r
        if isinstance(r, str):
            match = re.search(r'\d+', r.replace(',', '').replace('.', ''))
            return int(match.group()) if match else 0
        return 0

    all_items.sort(key=lambda x: get_reviews(x), reverse=True)

    for item in all_items:
        title = item.get('title', '')
        address = item.get('address', '')
        place_id = item.get('place_id', '')
        thumb = item.get('thumbnail_large') or item.get('thumbnail')
        
        norm_title = normalize(title)
        # Use first 15 chars of normalized address to avoid slight formatting differences
        norm_addr = normalize(address)[:15]

        is_dup = False
        
        # 1. Image Uniqueness Check
        if thumb and thumb in seen_images:
            # If same image but different title, one is likely wrong
            if similarity(normalize(seen_images[thumb]), norm_title) < 0.6:
                # print(f"Skipping {title} - image already used by {seen_images[thumb]}")
                continue
        
        # 2. Combined Title + Address check
        for existing in unique_items:
            # Check Place ID
            if place_id and existing.get('place_id') == place_id:
                is_dup = True; break
            
            # Check Fuzzy Title + Address
            ex_title = normalize(existing.get('title', ''))
            if similarity(ex_title, norm_title) > 0.85:
                # Same title, if address is similar or matching first 10 chars, it's a dup
                ex_addr = normalize(existing.get('address', ''))[:15]
                if not norm_addr or not ex_addr or similarity(ex_addr, norm_addr) > 0.7:
                    is_dup = True; break
        
        if not is_dup:
            if thumb: seen_images[thumb] = title
            unique_items.append(item)

    # Distribute back to files
    new_data = {
        "tourism_places.json": [],
        "cafe.json": [],
        "restaurant.json": []
    }

    for item in unique_items:
        origin = item.pop('_origin_file')
        new_data[origin].append(item)

    # Write back
    for f_name, items in new_data.items():
        f_path = os.path.join(full_dir, f_name)
        with open(f_path, 'w', encoding='utf-8') as fw:
            json.dump(items, fw, ensure_ascii=False, indent=2)
    
    print(f"Aggressive clean {city_folder}: Total unique items {len(unique_items)} (from {len(all_items)})")

def run_aggressive_clean(base_path):
    for city in os.listdir(base_path):
        if city in CITY_MAP:
            process_city(base_path, city)

if __name__ == "__main__":
    run_aggressive_clean("e:/Downloads/Backend_mobile-dev/Backend_mobile-dev/src/main/resources/city-data")
