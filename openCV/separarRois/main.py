import cv2
import numpy as np

stepByStep = True


def esperarProximaEtapa():
    if stepByStep:
        cv2.waitKey()
        cv2.destroyAllWindows()


image_filename = "arvores17.jpg"

imagem = cv2.imread(image_filename)
imagemCinza = cv2.cvtColor(imagem, cv2.COLOR_BGR2GRAY)


cv2.imshow("Imagem Original", imagemCinza)

esperarProximaEtapa()

#
# gausian blur
#

kernel_size = (15, 15)
sigmaX = 0

imagemPasaBaixa = cv2.GaussianBlur(imagemCinza, kernel_size, sigmaX)

cv2.imshow("Imagem gausian", imagemPasaBaixa)


esperarProximaEtapa()

#
# aplicando a tecnica de Otsu
#
_, imagemBinaria = cv2.threshold(
    imagemPasaBaixa, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU
)

#
# operações morfológicas
#

# definindo o tamanho do kernel para as operações morfológicas
kernel = cv2.getStructuringElement(cv2.MORPH_RECT, (7, 7))

# aplicando a operação de erosao
imagemDilatacao = cv2.erode(imagemBinaria, kernel, iterations=2)

# aplicando a operação de abertura
imagemFechamento = cv2.morphologyEx(
    imagemDilatacao, cv2.MORPH_OPEN, kernel, iterations=1
)

cv2.imshow("Imagem binarizada", imagemFechamento)

esperarProximaEtapa()


#
# invertendo a imagem
#

imagemInvertida = cv2.bitwise_not(imagemFechamento)

#
# identificando as regiões conectadas
#

_, labels = cv2.connectedComponents(imagemInvertida, connectivity=8)

# definindo o número de componentes conectados
num_labels = np.max(labels) + 1

# definindo a distância entre os cinzas
distacia_labels = 255 / num_labels - 1

print("Número de componentes conectados:", num_labels)

# gerando uma imagem em branco
componentes_cinzas = np.zeros_like(imagemInvertida, dtype=np.uint8)

# preenchendo a imagem com os componentes cinzas
for label in range(1, num_labels):
    # Gerar um tom de cinza diferente para cada componente
    # Usa o rótulo do componente para criar tons diferentes igualmente espaçados
    cinza = int(distacia_labels * label) if num_labels > 1 else 128

    # Criar uma máscara para o componente atual
    mask = (labels == label).astype(np.uint8)

    # Atribuir o tom de cinza à região do componente na nova imagem
    componentes_cinzas[mask == 1] = cinza


cv2.imshow("Imagem com componentes conectados", componentes_cinzas)

esperarProximaEtapa()

#
# Encontrando contornos
#

contornos, _ = cv2.findContours(
    componentes_cinzas, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE
)

# criando uma imagem para desenhar os contornos
contours_image = np.zeros_like(componentes_cinzas)

# desenha os contornos
cv2.drawContours(contours_image, contornos, -1, (255, 255, 255), 2)

# escrevendo o número da roi no centro de cada contorno
for i, contorno in enumerate(contornos):
    # calcula o centro de massa
    M = cv2.moments(contorno)
    if M["m00"] != 0:
        cx = int(M["m10"] / M["m00"])
        cy = int(M["m01"] / M["m00"])
        cv2.putText(
            contours_image,
            str(i),
            (cx, cy),
            cv2.FONT_HERSHEY_SIMPLEX,
            0.5,
            (255, 255, 255),
            1,
            cv2.LINE_AA,
        )

# mostrando os contornos
cv2.imshow("Contornos", contours_image)

esperarProximaEtapa()

#
# Salvando os contornos
#

for i, contorno in enumerate(contornos):
    # obtem o bounding box do retangulo
    x, y, w, h = cv2.boundingRect(contorno)

    roi = imagem[y : y + h, x : x + w]

    cv2.imwrite(f"out/{image_filename}_Roi_{i + 1}.jpg", roi)

    cv2.imshow(f"ROI {i + 1}", roi)

while True:
    k = cv2.waitKey(0)
    if k == 27:
        cv2.destroyAllWindows()
        break
