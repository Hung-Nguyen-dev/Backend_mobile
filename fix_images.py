import os
import json
import re
import hashlib

# Massive Pool of high-quality Unsplash IDs for maximum variety
# I will use a logic that makes each item have a unique visual fingerprint
TOURISM_IPS = [
    'photo-1528127269322-539801943592', 'photo-1506744038136-46273834b3fb', 'photo-1541529086526-db283c563270',
    'photo-1501339847302-ac426a4a7cbb', 'photo-1596402184320-417d7178b2cd', 'photo-1507525428034-b723cf961d3e',
    'photo-1542642235-9051066edfc2', 'photo-1470071459604-3b5ec3a7fe05', 'photo-1441974231531-c6227db76b6e',
    'photo-1501785888041-af3ef285b470', 'photo-1464822759023-fed622ff2c3b', 'photo-1469474968028-56623f02e42e',
    'photo-1500530855697-b586d89ba3ee', 'photo-1533619239233-628203c63e1f', 'photo-1537996194471-e657df975ab4',
    'photo-1506197603052-3cc9c3a201bd', 'photo-1533105079780-92b9be482077', 'photo-1512453979798-5ea266f8880c',
    'photo-1526312426976-f4d754de9bd6', 'photo-1523906834658-6e24ef2386f9', 'photo-1502602898657-3e91760cbb34'
]

REST_IPS = [
    'photo-1555126634-323283e090fa', 'photo-1504674900247-0877df9cc836', 'photo-1512621776951-a57141f2eefd',
    'photo-1476224203421-9ac39bc34273', 'photo-1493770348161-369560ae357d', 'photo-1473093226795-af9932fe5856',
    'photo-1567620905732-2d1ec7bb7445', 'photo-1565299624946-b28f40a0ae38', 'photo-1482049016688-2d3e1b311543',
    'photo-1484723091739-30a097e8f929', 'photo-1511690656952-34342bb7c2f2', 'photo-1515003197210-e0cd71810b5f',
    'photo-1546069901-ba9599a7e63c', 'photo-1565958011703-44f9829ba187'
]

CAFE_IPS = [
    'photo-1507133750040-4a8f5700e53f', 'photo-1495474472287-4d71bcdd2085', 'photo-1442512595334-71bc393173ce',
    'photo-1501339847302-ac426a4a7cbb', 'photo-1511920170033-f8396924c348', 'photo-1541167760496-1629557bd572',
    'photo-1498804103079-a6351b050096', 'photo-1497935586351-b67a49e012bf', 'photo-1509042239860-f550ce710b93',
    'photo-1554118811-1e0d58224f24', 'photo-1461023232487-21a468fd9bc7'
]

def slugify(text):
    text = text.lower()
    text = re.sub(r'[^a-z0-9]+', '-', text)
    return text.strip('-')

def get_unique_photo(title, category_pool):
    # Use MD5 of the title to get a deterministic but stable index for this specific place
    # This ensures "Sầm Sơn" always gets the same image even if re-run, 
    # but "Hồ Xuân Hương" gets a different one.
    import hashlib
    m = hashlib.md5(title.encode('utf-8')).hexdigest()
    idx = int(m, 16) % len(category_pool)
    return category_pool[idx], m[:6]

base_path = r'e:\Downloads\Backend_mobile-dev\Backend_mobile-dev\src\main\resources\city-data'

files_updated = 0
items_updated = 0

for root, dirs, files in os.walk(base_path):
    for file in files:
        if file.endswith('.json'):
            file_path = os.path.join(root, file)
            category = 'tourism'
            pool = TOURISM_IPS
            if 'cafe' in file: 
                category = 'cafe'
                pool = CAFE_IPS
            elif 'restaurant' in file: 
                category = 'restaurant'
                pool = REST_IPS
            
            with open(file_path, 'r', encoding='utf-8') as f:
                try:
                    data = json.load(f)
                except Exception as e:
                    print(f"Error loading {file}: {e}")
                    continue
            
            if 'local_results' in data:
                for item in data['local_results']:
                    title = item.get('title', 'unknown')
                    photo_id, sig_hash = get_unique_photo(title, pool)
                    # We add the title slug to the sig plus the unique hash
                    sig = f"{slugify(title)}-{sig_hash}"
                    item['thumbnail'] = f"https://images.unsplash.com/{photo_id}?q=80&w=400&auto=format&fit=crop&sig={sig}"
                    items_updated += 1
                
                with open(file_path, 'w', encoding='utf-8') as f:
                    json.dump(data, f, ensure_ascii=False, indent=2)
                
                files_updated += 1

print(f"Update complete. {files_updated} files and {items_updated} items updated with UNIQUE visual mapping.")
