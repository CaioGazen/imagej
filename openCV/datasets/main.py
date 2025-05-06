import cv2
import numpy as np
import os
import random

# --- Configuração ---
BACKGROUNDS_DIR = "backgroundSrc"  # Diretório contendo as imagens de fundo
OBJECTS_DIR = "imagesSrc"  # Diretório contendo as imagens de objetos segmentados (com canal alfa ou fundo branco)
OUTPUT_DIR = "hotwheels"  # Diretório de saída base
OUTPUT_IMAGES_DIR = os.path.join(
    OUTPUT_DIR, "images"
)  # Diretório de saída para as imagens geradas
OUTPUT_LABELS_DIR = os.path.join(
    OUTPUT_DIR, "labels"
)  # Diretório de saída para os rótulos YOLO

NUM_IMAGES_TO_GENERATE = 180
NUM_OBJECTS_PER_IMAGE = 3
CLASS_ID = 0  # ID da classe YOLO para o objeto (0 para 'hotwheel')

# Dimensões da imagem de destino (assumido do prompt)
IMG_WIDTH = 1024
IMG_HEIGHT = 768


# --- Função Auxiliar para Mistura ---
def paste_object(background, obj_img, x, y):
    """
    Args:
        background (np.ndarray): A imagem de fundo (3 canais).
        x (int): A coordenada x (canto superior esquerdo) para colar o objeto.
        y (int): A coordenada y (canto superior esquerdo) para colar o objeto.

    Returns:
        np.ndarray: A imagem de fundo com o objeto colado.
    """

    # obter as dimensões do objeto
    obj_h, obj_w = obj_img.shape[:2]

    # Garantir que a região de colagem esteja dentro dos limites do fundo
    x1, y1 = max(0, x), max(0, y)
    x2, y2 = min(background.shape[1], x + obj_w), min(background.shape[0], y + obj_h)

    # Calcular a região para colar (ROI no fundo)
    bg_roi = background[y1:y2, x1:x2]

    # Calcular a região correspondente da imagem do objeto
    obj_roi = obj_img  # a regiao do objeto é a imagem inteira

    # Criar máscara onde pixels NÃO SÃO brancos
    # Define os limites inferior e superior para a cor branca (BGR)
    lower_white = np.array(
        [250, 250, 250], dtype=np.uint8
    )  # Usando um pequeno limite para variação de branco
    upper_white = np.array([255, 255, 255], dtype=np.uint8)
    # Encontrar pixels dentro do intervalo de branco
    white_mask = cv2.inRange(obj_roi, lower_white, upper_white)
    # Inverter a máscara: 0 para branco (transparente), 255 para não branco (opaco)
    opaque_mask = cv2.bitwise_not(white_mask)

    # Criar máscara alfa normalizada (0.0 a 1.0)
    alpha = opaque_mask.astype(float) / 255.0
    alpha_mask = cv2.merge([alpha, alpha, alpha])

    # Misturar o objeto com a ROI do fundo usando a máscara de branco
    blended_roi = bg_roi * (1.0 - alpha_mask) + obj_roi * alpha_mask

    # Colocar a ROI misturada de volta no fundo
    background[y1:y2, x1:x2] = blended_roi.astype(np.uint8)

    return background


def generate_images(number_of_images, folderName):

    # Criar diretórios de saída se não existirem
    image_dir_path = os.path.join(OUTPUT_IMAGES_DIR, folderName)
    label_dir_path = os.path.join(OUTPUT_LABELS_DIR, folderName)

    os.makedirs(image_dir_path, exist_ok=True)
    os.makedirs(label_dir_path, exist_ok=True)

    for i in range(number_of_images):
        # Seleciona um fundo aleatório
        bg_path = random.choice(background_files)
        background = cv2.imread(bg_path, cv2.IMREAD_COLOR)  # Ler como RGB (3 canais)

        # Verificar se o carregamento do fundo foi bem-sucedido e redimensionar se necessário
        if background is None:
            print(
                f"Aviso: Não foi possível carregar a imagem de fundo {bg_path}. Pulando."
            )
            continue
        if background.shape[1] != IMG_WIDTH or background.shape[0] != IMG_HEIGHT:
            print(
                f"Aviso: O fundo {bg_path} tem dimensões {background.shape[1]}x{background.shape[0]}, esperado {IMG_WIDTH}x{IMG_HEIGHT}. Redimensionando."
            )
            background = cv2.resize(background, (IMG_WIDTH, IMG_HEIGHT))

        # Criar uma cópia para colar os objetos
        generated_image = background.copy()
        annotations = []  # Lista para armazenar anotações para esta imagem

        # Selecionar objetos aleatórios e colá-los
        selected_objects_paths = random.choices(object_files, k=NUM_OBJECTS_PER_IMAGE)

        for obj_path in selected_objects_paths:
            obj_img = cv2.imread(obj_path)  # Tentar ler

            if obj_img is None:
                print(
                    f"Aviso: Não foi possível carregar a imagem do objeto {obj_path}. Pulando."
                )
                continue

            # Verificar se a imagem tem 3 canais
            if obj_img.shape[2] != 3:
                print(
                    f"Aviso: A imagem do objeto {obj_path} tem {obj_img.shape[2]} canais, esperado 3. Pulando."
                )
                continue

            obj_h, obj_w = obj_img.shape[:2]

            # Garantir que o objeto não seja maior que o fundo
            if obj_w > IMG_WIDTH or obj_h > IMG_HEIGHT:
                print(
                    f"Aviso: O objeto {obj_path} ({obj_w}x{obj_h}) é maior que o esperado ({IMG_WIDTH}x{IMG_HEIGHT}). Redimensionando."
                )
                continue

            # Calcular posição de colagem aleatória (canto superior esquerdo)
            # Garantir que o objeto inteiro caiba dentro do fundo
            max_paste_x = IMG_WIDTH - obj_w
            max_paste_y = IMG_HEIGHT - obj_h

            if max_paste_x < 0 or max_paste_y < 0:
                # Isso não deveria acontecer se a verificação anterior passou
                print(
                    f"Erro ao calcular limites de posição de colagem para {obj_path}. Pulando."
                )
                continue

            paste_x = random.randint(0, max_paste_x)
            paste_y = random.randint(0, max_paste_y)

            # Colar o objeto
            paste_object(generated_image, obj_img, paste_x, paste_y)

            # Calcular e armazenar a anotação YOLO
            # Formato YOLO: class_id center_x center_y width height (todos normalizados)
            center_x = (paste_x + obj_w / 2.0) / IMG_WIDTH
            center_y = (paste_y + obj_h / 2.0) / IMG_HEIGHT
            width_normalized = obj_w / IMG_WIDTH
            height_normalized = obj_h / IMG_HEIGHT

            annotations.append(
                f"{CLASS_ID} {center_x:.6f} {center_y:.6f} {width_normalized:.6f} {height_normalized:.6f}"
            )

        # Salvar a imagem gerada e o arquivo de anotação
        image_filename = f"image_{i:03d}.png"  # Ex: image_000.png, image_001.png
        label_filename = f"image_{i:03d}.txt"  # Ex: image_000.txt, image_001.txt

        image_path = os.path.join(image_dir_path, image_filename)
        label_path = os.path.join(label_dir_path, label_filename)

        cv2.imwrite(image_path, generated_image)

        with open(label_path, "w") as f:
            for annotation in annotations:
                f.write(annotation + "\n")

        if (i + 1) % 10 == 0:
            print(f"Geradas {i + 1}/{number_of_images} imagens e rótulos.")


def generated_data_yml():

    yml_path = os.path.join(OUTPUT_DIR, "data.yaml")

    yml = f"path: ../../datasets/{OUTPUT_DIR}\n\ntrain: images/train\nval: images/val\n\nnames:\n   0: hotwheels"

    with open(yml_path, "w") as f:
        f.write(yml)


# --- Script Principal ---
if __name__ == "__main__":

    # Obter lista de arquivos de fundo e objetos
    background_files = [
        os.path.join(BACKGROUNDS_DIR, f)
        for f in os.listdir(BACKGROUNDS_DIR)
        if f.lower().endswith((".png", ".jpg", ".jpeg", ".bmp"))
    ]
    object_files = [
        os.path.join(OBJECTS_DIR, f)
        for f in os.listdir(OBJECTS_DIR)
        if f.lower().endswith((".png", ".jpg", ".jpeg", ".bmp"))
    ]

    if not background_files:
        print(f"Erro: Nenhum arquivo de imagem encontrado em {BACKGROUNDS_DIR}")
        exit()
    if not object_files:
        print(f"Erro: Nenhum arquivo de imagem encontrado em {OBJECTS_DIR}")
        exit()

    print(f"Encontradas {len(background_files)} imagens de fundo.")
    print(f"Encontradas {len(object_files)} imagens de objeto.")

    generate_images(NUM_IMAGES_TO_GENERATE, "train")
    generate_images(20, "val")

    generated_data_yml()

    print(f"\nFinalizada a geração de {NUM_IMAGES_TO_GENERATE} imagens e rótulos.")
    print(f"Imagens salvas em: {OUTPUT_IMAGES_DIR}")
    print(f"Rótulos salvos em: {OUTPUT_LABELS_DIR}")
