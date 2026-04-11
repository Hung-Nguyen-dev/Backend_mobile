import os
import json
from collections import defaultdict

def find_duplicate_images(base_path):
    image_to_places = defaultdict(list)
    
    for root, dirs, files in os.walk(base_path):
        for file in files:
            if file.endswith('.json'):
                path = os.path.join(root, file)
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        data = json.load(f)
                        city = os.path.basename(root)
                        category = file.replace('.json', '')
                        
                        # Handle list of places
                        places = []
                        if isinstance(data, list):
                            places = data
                        elif isinstance(data, dict):
                            # Some files might have a root key
                            for key in data:
                                if isinstance(data[key], list):
                                    places = data[key]
                                    break
                        
                        for place in places:
                            name = place.get('title', place.get('name', 'Unknown'))
                            thumb = place.get('thumbnail')
                            thumb_large = place.get('thumbnail_large')
                            
                            if thumb:
                                image_to_places[thumb].append(f"{city}/{category}: {name}")
                            if thumb_large:
                                image_to_places[thumb_large].append(f"{city}/{category}: {name}")
                except Exception as e:
                    print(f"Error reading {path}: {e}")

    print("\n--- Duplicate Images Found ---")
    duplicate_count = 0
    for img, place_list in image_to_places.items():
        if len(place_list) > 1:
            # Filter out generic icons if any (e.g. empty strings or known placeholders)
            if not img or "placeholder" in img:
                continue
                
            print(f"Image: {img}")
            for p in place_list:
                print(f"  - {p}")
            duplicate_count += 1
            print("-" * 20)
            
    print(f"\nTotal duplicate image groups found: {duplicate_count}")

if __name__ == "__main__":
    find_duplicate_images("e:/Downloads/Backend_mobile-dev/Backend_mobile-dev/src/main/resources/city-data")
