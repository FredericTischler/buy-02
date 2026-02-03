#!/bin/bash
# Script de seed pour alimenter la base de données e-commerce
# Usage: bash seed-data.sh

BASE_URL="http://localhost:8081"
PRODUCT_URL="http://localhost:8082"

echo "=== Seed de la base de données e-commerce ==="
echo ""

# --- Création des vendeurs ---
echo ">> Création des vendeurs..."

register_user() {
  local name="$1" email="$2" password="$3" role="$4"
  curl -s -X POST "$BASE_URL/api/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"$name\",\"email\":\"$email\",\"password\":\"$password\",\"role\":\"$role\"}" \
    > /dev/null 2>&1
  echo "   Enregistré: $name ($role)"
}

login_user() {
  local email="$1" password="$2"
  curl -s -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"$password\"}" 2>/dev/null
}

# Vendeurs
register_user "TechVision Store" "techvision@shop.com" "Seller123!" "SELLER"
register_user "ModaChic Boutique" "modachic@shop.com" "Seller123!" "SELLER"
register_user "SportZone Pro" "sportzone@shop.com" "Seller123!" "SELLER"
register_user "MaisonDeco Shop" "maisondeco@shop.com" "Seller123!" "SELLER"
register_user "BookWorm Library" "bookworm@shop.com" "Seller123!" "SELLER"

# Clients
register_user "Alice Martin" "alice@client.com" "Client123!" "CLIENT"
register_user "Bob Dupont" "bob@client.com" "Client123!" "CLIENT"
register_user "Claire Moreau" "claire@client.com" "Client123!" "CLIENT"

echo ""
echo ">> Connexion des vendeurs..."

# Login vendeurs et récupération des tokens
TECH_TOKEN=$(login_user "techvision@shop.com" "Seller123!" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)
MODA_TOKEN=$(login_user "modachic@shop.com" "Seller123!" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)
SPORT_TOKEN=$(login_user "sportzone@shop.com" "Seller123!" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)
MAISON_TOKEN=$(login_user "maisondeco@shop.com" "Seller123!" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)
BOOK_TOKEN=$(login_user "bookworm@shop.com" "Seller123!" | python3 -c "import sys,json; print(json.load(sys.stdin).get('token',''))" 2>/dev/null)

# Vérifier qu'on a au moins un token
if [ -z "$TECH_TOKEN" ]; then
  echo "ERREUR: Impossible de se connecter. Vérifiez que le user-service tourne sur $BASE_URL"
  exit 1
fi

echo "   Tokens récupérés avec succès"
echo ""

# --- Création des produits ---
create_product() {
  local token="$1" name="$2" desc="$3" price="$4" category="$5" stock="$6"
  local result
  result=$(curl -s -X POST "$PRODUCT_URL/api/products" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $token" \
    -d "{\"name\":\"$name\",\"description\":\"$desc\",\"price\":$price,\"category\":\"$category\",\"stock\":$stock}" 2>/dev/null)

  local id=$(echo "$result" | python3 -c "import sys,json; print(json.load(sys.stdin).get('id','ERREUR'))" 2>/dev/null)
  if [ "$id" != "ERREUR" ] && [ -n "$id" ]; then
    echo "   + $name ($category) - ${price}€"
  else
    echo "   x ERREUR: $name - $result"
  fi
}

# ============================================
# TECHVISION STORE - Électronique & High-Tech
# ============================================
echo ">> Produits TechVision Store (Électronique)..."

create_product "$TECH_TOKEN" "MacBook Pro 16 M3 Pro" "MacBook Pro 16 pouces avec puce M3 Pro, 18 Go RAM, 512 Go SSD. Écran Liquid Retina XDR. Parfait pour les professionnels créatifs." 2799.99 "Laptops" 25
create_product "$TECH_TOKEN" "MacBook Air 15 M3" "MacBook Air 15 pouces, puce M3, 8 Go RAM, 256 Go SSD. Ultra-fin et léger, autonomie exceptionnelle de 18h." 1499.99 "Laptops" 40
create_product "$TECH_TOKEN" "Dell XPS 15" "Dell XPS 15 avec Intel Core i7-13700H, 16 Go RAM, 512 Go SSD, écran OLED 3.5K tactile. Design premium." 1899.99 "Laptops" 15
create_product "$TECH_TOKEN" "Lenovo ThinkPad X1 Carbon" "ThinkPad X1 Carbon Gen 11, Intel Core i7, 16 Go RAM, 512 Go SSD. Le laptop professionnel par excellence." 1699.99 "Laptops" 20
create_product "$TECH_TOKEN" "ASUS ROG Strix G16" "PC portable gaming ROG Strix G16, Intel i9, RTX 4070, 32 Go RAM, 1 To SSD. Écran 240Hz QHD." 2199.99 "Gaming" 12

create_product "$TECH_TOKEN" "iPhone 16 Pro Max" "iPhone 16 Pro Max 256 Go, puce A18 Pro, écran Super Retina XDR 6.9 pouces, titane naturel." 1479.99 "Smartphones" 60
create_product "$TECH_TOKEN" "Samsung Galaxy S24 Ultra" "Galaxy S24 Ultra 256 Go, Snapdragon 8 Gen 3, S Pen intégré, écran Dynamic AMOLED 2X 6.8 pouces." 1419.99 "Smartphones" 45
create_product "$TECH_TOKEN" "Google Pixel 9 Pro" "Pixel 9 Pro 128 Go, puce Tensor G4, appareil photo 50MP avec IA, écran LTPO OLED 6.3 pouces." 1099.99 "Smartphones" 30
create_product "$TECH_TOKEN" "OnePlus 12" "OnePlus 12, Snapdragon 8 Gen 3, 16 Go RAM, 256 Go, charge 100W, écran 2K LTPO AMOLED 120Hz." 899.99 "Smartphones" 35
create_product "$TECH_TOKEN" "Xiaomi 14 Ultra" "Xiaomi 14 Ultra, optique Leica Summilux, Snapdragon 8 Gen 3, 16 Go RAM, 512 Go. Photo professionnelle." 1299.99 "Smartphones" 20

create_product "$TECH_TOKEN" "iPad Pro M4 12.9" "iPad Pro 12.9 pouces avec puce M4, écran tandem OLED, 256 Go. La tablette la plus puissante du marché." 1399.99 "Tablets" 30
create_product "$TECH_TOKEN" "Samsung Galaxy Tab S9 Ultra" "Galaxy Tab S9 Ultra 14.6 pouces, Snapdragon 8 Gen 2, 12 Go RAM, 256 Go, S Pen inclus." 1179.99 "Tablets" 18

create_product "$TECH_TOKEN" "Sony WH-1000XM5" "Casque sans fil à réduction de bruit adaptative, 30h d'autonomie, audio Hi-Res, appels cristallins." 349.99 "Headphones" 80
create_product "$TECH_TOKEN" "Apple AirPods Pro 2" "AirPods Pro 2ème génération avec boîtier USB-C, réduction de bruit active, audio spatial personnalisé." 279.99 "Headphones" 100
create_product "$TECH_TOKEN" "Bose QuietComfort Ultra" "Casque Bose QC Ultra, réduction de bruit immersive, audio spatial, 24h d'autonomie, confort premium." 399.99 "Headphones" 45
create_product "$TECH_TOKEN" "JBL Charge 5" "Enceinte Bluetooth portable, 20h d'autonomie, étanche IP67, son JBL Original Pro, powerbank intégrée." 149.99 "Headphones" 70

create_product "$TECH_TOKEN" "Sony PlayStation 5 Slim" "PS5 Slim édition standard avec lecteur disque, 1 To SSD, manette DualSense. Jouez aux derniers jeux next-gen." 549.99 "Gaming" 25
create_product "$TECH_TOKEN" "Nintendo Switch OLED" "Nintendo Switch modèle OLED, écran 7 pouces, dock avec port Ethernet, 64 Go de stockage." 349.99 "Gaming" 40
create_product "$TECH_TOKEN" "Xbox Series X" "Console Microsoft Xbox Series X, 1 To SSD, 4K 120fps, rétrocompatible, Game Pass Ultimate inclus 1 mois." 499.99 "Gaming" 20
create_product "$TECH_TOKEN" "Logitech G Pro X Superlight 2" "Souris gaming sans fil ultra-légère 60g, capteur HERO 2, 95h d'autonomie, switches optiques." 159.99 "Gaming" 55
create_product "$TECH_TOKEN" "Razer BlackWidow V4 Pro" "Clavier mécanique gaming, switches Razer Green, repose-poignet magnétique, rétroéclairage RGB Chroma." 229.99 "Gaming" 35

create_product "$TECH_TOKEN" "LG C3 OLED 55 pouces" "TV OLED 55 pouces, 4K 120Hz, Dolby Vision & Atmos, webOS 23, HDMI 2.1, idéal gaming et cinéma." 1299.99 "Electronics" 10
create_product "$TECH_TOKEN" "Apple Watch Ultra 2" "Apple Watch Ultra 2, boîtier titane 49mm, GPS + Cellular, 72h d'autonomie, étanche 100m." 899.99 "Electronics" 25
create_product "$TECH_TOKEN" "GoPro HERO 12 Black" "Caméra d'action 5.3K60, stabilisation HyperSmooth 6.0, étanche 10m, HDR, commande vocale." 399.99 "Electronics" 30
create_product "$TECH_TOKEN" "DJI Mini 4 Pro" "Drone compact 249g, caméra 4K HDR, détection d'obstacles omnidirectionnelle, 34 min de vol." 799.99 "Electronics" 15

# ============================================
# MODACHIC BOUTIQUE - Mode & Accessoires
# ============================================
echo ">> Produits ModaChic Boutique (Mode)..."

create_product "$MODA_TOKEN" "Veste en cuir vintage" "Veste en cuir véritable style vintage, doublure intérieure en satin, poches zippées. Coupe ajustée." 249.99 "Fashion" 30
create_product "$MODA_TOKEN" "Jean slim Selvedge brut" "Jean slim en denim selvedge brut japonais 14oz, coupe slim, non délavé. Se patine avec le temps." 129.99 "Fashion" 50
create_product "$MODA_TOKEN" "Pull cachemire col V" "Pull en pur cachemire, col V classique, maille fine. Disponible en noir, gris, bleu marine et bordeaux." 189.99 "Fashion" 40
create_product "$MODA_TOKEN" "Chemise Oxford blanche" "Chemise Oxford en coton premium, col boutonné, coupe regular. Un indispensable du vestiaire masculin." 79.99 "Fashion" 60
create_product "$MODA_TOKEN" "Robe midi plissée" "Robe midi plissée en mousseline, taille élastiquée, doublée. Élégante pour toutes les occasions." 119.99 "Fashion" 35
create_product "$MODA_TOKEN" "Manteau laine oversize" "Manteau long en laine mélangée, coupe oversize, col tailleur, fermeture croisée. Chic et chaud." 299.99 "Fashion" 20
create_product "$MODA_TOKEN" "T-shirt coton bio premium" "T-shirt en coton biologique 240g, coupe droite, col rond renforcé. Doux et durable." 39.99 "Fashion" 150
create_product "$MODA_TOKEN" "Blazer en lin naturel" "Blazer en lin naturel, coupe décontractée, 2 boutons, poches plaquées. Parfait pour l'été." 179.99 "Fashion" 25
create_product "$MODA_TOKEN" "Pantalon chino stretch" "Chino en coton stretch, coupe droite, taille mi-haute. Confortable au quotidien. Plusieurs coloris." 89.99 "Fashion" 70
create_product "$MODA_TOKEN" "Doudoune légère compressible" "Doudoune fine en nylon déperlant, garnissage duvet, pochette de rangement incluse. Ultra-légère." 149.99 "Fashion" 45

create_product "$MODA_TOKEN" "Sneakers cuir blanc minimaliste" "Sneakers en cuir pleine fleur blanc, semelle cousue, doublure cuir. Design épuré et intemporel." 159.99 "Shoes" 40
create_product "$MODA_TOKEN" "Bottines Chelsea en daim" "Bottines Chelsea en daim véritable, semelle en crêpe, boucle arrière. Style britannique classique." 199.99 "Shoes" 25
create_product "$MODA_TOKEN" "Baskets running légères" "Baskets de running avec semelle en mousse EVA, tige en mesh respirant, drop 8mm. Polyvalentes." 119.99 "Shoes" 55
create_product "$MODA_TOKEN" "Mocassins en cuir souple" "Mocassins en cuir nappa souple, semelle flexible, cousu Blake. Élégance décontractée." 139.99 "Shoes" 30
create_product "$MODA_TOKEN" "Sandales cuir artisanales" "Sandales en cuir tanné végétal, fabrication artisanale, semelle anatomique. Confort estival." 89.99 "Shoes" 35

create_product "$MODA_TOKEN" "Sac à dos cuir grainé" "Sac à dos en cuir grainé, compartiment laptop 15 pouces, poches organisées, fermeture magnétique." 219.99 "Accessories" 20
create_product "$MODA_TOKEN" "Montre automatique acier" "Montre automatique mouvement Miyota, boîtier acier 40mm, verre saphir, bracelet cuir. Étanche 50m." 349.99 "Accessories" 15
create_product "$MODA_TOKEN" "Ceinture cuir réversible" "Ceinture en cuir réversible noir/marron, boucle classique en métal brossé. Deux looks en un." 59.99 "Accessories" 80
create_product "$MODA_TOKEN" "Écharpe laine mérinos" "Écharpe en laine mérinos extra-fine, tissage sergé, franges nouées. Douce et chaude." 69.99 "Accessories" 50
create_product "$MODA_TOKEN" "Lunettes de soleil polarisées" "Lunettes de soleil en acétate, verres polarisés catégorie 3, protection UV400. Style rétro." 129.99 "Accessories" 40
create_product "$MODA_TOKEN" "Portefeuille cuir RFID" "Portefeuille compact en cuir, protection RFID, 6 emplacements cartes, compartiment billets." 49.99 "Accessories" 60

# ============================================
# SPORTZONE PRO - Sport & Outdoor
# ============================================
echo ">> Produits SportZone Pro (Sport)..."

create_product "$SPORT_TOKEN" "Vélo de route carbone Shimano 105" "Vélo de route cadre carbone T700, groupe Shimano 105, roues alu, freins à disque hydrauliques. Taille M." 1899.99 "Sports" 8
create_product "$SPORT_TOKEN" "Tapis de yoga premium 6mm" "Tapis de yoga antidérapant en TPE écologique, 183x66cm, épaisseur 6mm. Inclut sangle de transport." 49.99 "Sports" 100
create_product "$SPORT_TOKEN" "Haltères réglables 2-24kg (paire)" "Paire d'haltères réglables de 2 à 24kg, mécanisme de sélection rapide, base de rangement incluse." 349.99 "Sports" 20
create_product "$SPORT_TOKEN" "Raquette de tennis Wilson Pro Staff" "Raquette Wilson Pro Staff 97 v14, 315g, tamis 97in², profil 21.5mm. Non cordée." 249.99 "Sports" 15
create_product "$SPORT_TOKEN" "Sac de frappe 120cm" "Sac de frappe professionnel 120cm, rembourrage multi-couches, fixation plafond incluse. 40kg." 179.99 "Sports" 12
create_product "$SPORT_TOKEN" "Ballon de football adidas UCL" "Ballon officiel UEFA Champions League, taille 5, cousu main, technologie CTR-DISC." 49.99 "Sports" 80
create_product "$SPORT_TOKEN" "Corde à sauter speed" "Corde à sauter speed avec câble acier gainé, poignées alu ergonomiques, roulements à billes." 29.99 "Sports" 120
create_product "$SPORT_TOKEN" "Banc de musculation inclinable" "Banc de musculation réglable 7 positions, charge max 300kg, structure acier renforcé. Pliable." 249.99 "Sports" 10
create_product "$SPORT_TOKEN" "Gants de boxe 12oz" "Gants de boxe en cuir synthétique, rembourrage mousse multi-densité, fermeture velcro. 12oz." 59.99 "Sports" 40
create_product "$SPORT_TOKEN" "Tracker fitness GPS" "Bracelet connecté avec GPS intégré, suivi cardiaque, SpO2, sommeil, 14 jours d'autonomie. Étanche 50m." 149.99 "Sports" 55

create_product "$SPORT_TOKEN" "Tente de randonnée 2 personnes" "Tente igloo 2 places ultra-légère 1.8kg, double-toit imperméable 3000mm, montage rapide." 199.99 "Outdoor" 18
create_product "$SPORT_TOKEN" "Sac de couchage -10°C" "Sac de couchage momie, garnissage synthétique, température confort -5°C, extrême -10°C. 1.2kg." 129.99 "Outdoor" 25
create_product "$SPORT_TOKEN" "Sac à dos randonnée 50L" "Sac à dos 50L, dos ventilé, ceinture lombaire, accès fond, housse de pluie intégrée." 139.99 "Outdoor" 30
create_product "$SPORT_TOKEN" "Jumelles compactes 10x42" "Jumelles 10x42, prisme BaK-4, traitement multicouches, étanches, pour observation nature et rando." 179.99 "Outdoor" 15
create_product "$SPORT_TOKEN" "Lampe frontale rechargeable 1000lm" "Lampe frontale LED 1000 lumens, 5 modes, rechargeable USB-C, étanche IPX6, 8h d'autonomie." 44.99 "Outdoor" 60
create_product "$SPORT_TOKEN" "Chaussures de trail imperméables" "Chaussures de trail avec membrane imperméable, semelle Vibram, protection orteils. Adhérence maximale." 149.99 "Outdoor" 35
create_product "$SPORT_TOKEN" "Bâtons de randonnée carbone" "Paire de bâtons télescopiques en carbone, poignées liège, système de serrage rapide. 200g pièce." 79.99 "Outdoor" 40
create_product "$SPORT_TOKEN" "Gourde isotherme 1L" "Gourde isotherme double paroi inox, 24h froid / 12h chaud, bouchon sport, sans BPA." 34.99 "Outdoor" 90

# ============================================
# MAISONDECO SHOP - Maison & Déco
# ============================================
echo ">> Produits MaisonDeco Shop (Maison)..."

create_product "$MAISON_TOKEN" "Machine à café expresso automatique" "Machine expresso automatique avec broyeur intégré, 15 bars, mousseur à lait, écran tactile." 599.99 "Home" 15
create_product "$MAISON_TOKEN" "Robot aspirateur laveur" "Robot aspirateur et laveur 2-en-1, navigation laser LiDAR, 5000Pa, vidage automatique, app smartphone." 449.99 "Home" 20
create_product "$MAISON_TOKEN" "Blender haute puissance 2L" "Blender professionnel 2000W, bol Tritan 2L, 10 vitesses, programmes automatiques, lames inox 6 branches." 199.99 "Home" 30
create_product "$MAISON_TOKEN" "Purificateur d'air HEPA" "Purificateur d'air avec filtre HEPA H13, capteur qualité d'air, mode silencieux 24dB, couvre 60m²." 249.99 "Home" 18
create_product "$MAISON_TOKEN" "Bouilloire électrique col de cygne" "Bouilloire col de cygne 1L, contrôle température digital, maintien au chaud, pour café filtre et thé." 79.99 "Home" 45
create_product "$MAISON_TOKEN" "Poêle en fer forgé 28cm" "Poêle en fer forgé artisanale 28cm, compatible tous feux dont induction, culottage naturel. Durable." 69.99 "Home" 40
create_product "$MAISON_TOKEN" "Set de couteaux japonais 3 pièces" "Set de 3 couteaux japonais (Santoku, Nakiri, Petty) en acier VG-10, manches pakkawood. Coffret bois." 189.99 "Home" 20
create_product "$MAISON_TOKEN" "Cocotte en fonte émaillée 5.5L" "Cocotte ovale en fonte émaillée 5.5L, couvercle auto-arrosant, compatible four 260°C. Coloris cerise." 159.99 "Home" 25
create_product "$MAISON_TOKEN" "Mixeur plongeant 4-en-1" "Mixeur plongeant 1000W avec fouet, hachoir, pied mixeur et gobelet doseur. Vitesse variable." 69.99 "Home" 50
create_product "$MAISON_TOKEN" "Grille-pain artisan 2 fentes" "Grille-pain rétro 2 fentes larges, 7 niveaux de brunissage, fonctions décongélation et réchauffage." 89.99 "Home" 35

create_product "$MAISON_TOKEN" "Lampe de bureau LED articulée" "Lampe de bureau LED 12W, bras articulé aluminium, 5 températures de couleur, port USB, variateur." 79.99 "Furniture" 40
create_product "$MAISON_TOKEN" "Fauteuil de bureau ergonomique" "Fauteuil ergonomique mesh, support lombaire ajustable, accoudoirs 4D, appuie-tête, roulettes silencieuses." 449.99 "Furniture" 12
create_product "$MAISON_TOKEN" "Étagère murale chêne massif" "Étagère murale en chêne massif 80x20cm, finition huile naturelle, fixations invisibles incluses." 59.99 "Furniture" 50
create_product "$MAISON_TOKEN" "Bureau debout électrique 140cm" "Bureau assis-debout motorisé 140x70cm, mémorisation 4 positions, plateau bambou, anti-collision." 549.99 "Furniture" 8
create_product "$MAISON_TOKEN" "Miroir rond métal doré 60cm" "Miroir mural rond diamètre 60cm, cadre métal doré brossé, fixation murale. Style scandinave." 89.99 "Furniture" 25
create_product "$MAISON_TOKEN" "Coussin décoratif velours 45cm" "Coussin en velours côtelé 45x45cm, garnissage moelleux, fermeture zip invisible. Coloris terracotta." 29.99 "Furniture" 80
create_product "$MAISON_TOKEN" "Tapis berbère 160x230cm" "Tapis style berbère en polypropylène, 160x230cm, motifs losanges, poils courts faciles d'entretien." 179.99 "Furniture" 15
create_product "$MAISON_TOKEN" "Bougie parfumée artisanale 300g" "Bougie parfumée en cire de soja naturelle 300g, mèche coton, 60h de combustion. Senteur bois de santal." 34.99 "Furniture" 70

# ============================================
# BOOKWORM LIBRARY - Livres & Culture
# ============================================
echo ">> Produits BookWorm Library (Livres)..."

create_product "$BOOK_TOKEN" "Dune - Frank Herbert" "Dune, le chef-d'œuvre de la science-fiction. Édition collector reliée, couverture toilée, 896 pages." 29.99 "Books" 60
create_product "$BOOK_TOKEN" "Le Petit Prince - Saint-Exupéry" "Le Petit Prince, édition illustrée originale en couleurs, couverture rigide. Un classique intemporel." 14.99 "Books" 100
create_product "$BOOK_TOKEN" "Clean Code - Robert C. Martin" "Clean Code: A Handbook of Agile Software Craftsmanship. La bible du code propre pour tout développeur." 39.99 "Books" 45
create_product "$BOOK_TOKEN" "Sapiens - Yuval Noah Harari" "Sapiens: Une brève histoire de l'humanité. Édition poche, 512 pages. Comprendre notre espèce." 12.99 "Books" 80
create_product "$BOOK_TOKEN" "L'Étranger - Albert Camus" "L'Étranger d'Albert Camus, édition Folio, 192 pages. Le roman existentialiste fondateur." 7.99 "Books" 90
create_product "$BOOK_TOKEN" "The Pragmatic Programmer" "The Pragmatic Programmer: 20th Anniversary Edition. Conseils intemporels pour devenir meilleur dev." 44.99 "Books" 35
create_product "$BOOK_TOKEN" "1984 - George Orwell" "1984 de George Orwell, édition intégrale. Le roman dystopique le plus célèbre. 384 pages." 9.99 "Books" 75
create_product "$BOOK_TOKEN" "Designing Data-Intensive Applications" "DDIA par Martin Kleppmann. Architectures de systèmes distribués, Big Data, et bases de données." 49.99 "Books" 25
create_product "$BOOK_TOKEN" "Le Seigneur des Anneaux - Intégrale" "L'intégrale du Seigneur des Anneaux de Tolkien, édition illustrée par Alan Lee. Coffret 3 volumes." 59.99 "Books" 30
create_product "$BOOK_TOKEN" "Atomic Habits - James Clear" "Atomic Habits: Un rien peut tout changer. Méthode pour créer de bonnes habitudes. Bestseller mondial." 19.99 "Books" 65
create_product "$BOOK_TOKEN" "Les Misérables - Victor Hugo" "Les Misérables, édition intégrale en 2 tomes, collection Pléiade. L'œuvre magistrale de Victor Hugo." 34.99 "Books" 40
create_product "$BOOK_TOKEN" "System Design Interview" "System Design Interview par Alex Xu. Préparez vos entretiens techniques avec des cas concrets." 36.99 "Books" 30

create_product "$BOOK_TOKEN" "Vinyle Pink Floyd Dark Side of the Moon" "Dark Side of the Moon de Pink Floyd, vinyle 180g remasterisé, pochette gatefold originale." 34.99 "Music" 20
create_product "$BOOK_TOKEN" "Vinyle Daft Punk Random Access Memories" "Random Access Memories de Daft Punk, double vinyle 180g, édition limitée." 44.99 "Music" 15
create_product "$BOOK_TOKEN" "Platine vinyle Audio-Technica AT-LP60X" "Platine vinyle automatique, entraînement par courroie, préampli intégré, sortie RCA. Noir mat." 149.99 "Music" 18
create_product "$BOOK_TOKEN" "Enceintes bibliothèque Edifier R1280T" "Paire d'enceintes actives 2.0, 42W RMS, entrées RCA, télécommande. Son chaud et détaillé." 99.99 "Music" 22

echo ""

# --- Résumé ---
TOTAL=$(curl -s http://localhost:8082/api/products | python3 -c "import sys,json; print(len(json.load(sys.stdin)))" 2>/dev/null)
echo "=== Seed terminé ! ==="
echo ">> Total de produits en base : $TOTAL"
echo ""
echo "Comptes de test créés :"
echo "  Vendeurs :"
echo "    - techvision@shop.com  / Seller123!"
echo "    - modachic@shop.com    / Seller123!"
echo "    - sportzone@shop.com   / Seller123!"
echo "    - maisondeco@shop.com  / Seller123!"
echo "    - bookworm@shop.com    / Seller123!"
echo "  Clients :"
echo "    - alice@client.com     / Client123!"
echo "    - bob@client.com       / Client123!"
echo "    - claire@client.com    / Client123!"