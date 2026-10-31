# Wi-Fi Studio

Application de bureau Java Swing pour Windows permettant de découvrir les réseaux Wi-Fi disponibles et d’estimer le débit Internet descendant.

Développée en **Java 8**, compatible avec **NetBeans 8.0.2**, sans dépendance externe.

## Fonctionnalités

- **Scan Wi-Fi** : affichage des réseaux visibles et des informations fournies par Windows : SSID, BSSID, signal, canal et sécurité.
- **Connexion actuelle** : consultation des informations de l’interface Wi-Fi.
- **Test de débit descendant** : estimation en Mbit/s et en Mo/s.
- **Serveurs de secours** : essai automatique des serveurs Hetzner si Cloudflare refuse la requête ou reste inaccessible.
- **Indicateurs dédiés** : débit mesuré, volume reçu et serveur utilisé.
- **Interface réactive** : exécution des opérations en arrière-plan avec `SwingWorker`.
- **Gestion des accents** : lecture UTF-8 de la sortie Windows avec un repli CP850 pour les anciens systèmes.

## Technologies

| Élément | Technologie |
|---|---|
| Langage | Java 8 |
| Interface graphique | Java Swing |
| Environnement de développement | NetBeans 8.0.2 |
| Informations Wi-Fi | Commande Windows `netsh wlan` |
| Transfert de test | HTTPS avec `HttpURLConnection` |
| Dépendances externes | Aucune |

## Prérequis

- Windows avec une carte Wi-Fi active pour le scan.
- JDK 8 installé.
- Accès Internet pour le test de débit.
- NetBeans 8.0.2 si vous souhaitez utiliser cet environnement.

Un JDK 8 à jour est recommandé pour la compatibilité HTTPS.

## Installation avec NetBeans

1. Créez un projet **Java → Java Application**.
2. Sélectionnez le **JDK 8** et le format de sources **1.8**.
3. Créez une classe `WifiSpeedApp`, sans package.
4. Remplacez son contenu par celui du fichier `WifiSpeedApp.java`.
5. Dans **Project Properties → Sources → Encoding**, sélectionnez **UTF-8**.
6. Faites **Clean and Build**, puis exécutez la classe.

## Exécution en ligne de commande

Depuis le dossier contenant `WifiSpeedApp.java` :

```bash
javac -encoding UTF-8 WifiSpeedApp.java
java WifiSpeedApp
```

## Utilisation

### Découvrir les réseaux

Cliquez sur **Scanner le Wi-Fi**.

L’onglet **Réseaux Wi-Fi** affiche les informations retournées par Windows.

Si Windows refuse l’accès aux informations Wi-Fi, vérifiez les autorisations de localisation ainsi que l’activation de votre carte sans fil.
<img width="803" height="673" alt="Capture d&#39;écran 2026-10-31 132403" src="https://github.com/user-attachments/assets/65ba4555-2e8b-407c-a7f6-173a26cc3d93" />


### Mesurer le débit

1. Connectez Windows au réseau Wi-Fi à tester.
2. Cliquez sur **Tester le débit**.
3. Consultez les indicateurs et les détails dans l’onglet **Test de débit**.

Chaque test lit au maximum **25 Mo**. Le transfert s’arrête lorsque cette limite est atteinte ou après environ **15 secondes de mesure**. Une lecture en cours peut prolonger ce délai.
<img width="803" height="673" alt="Capture d&#39;écran 2026-10-31 132342" src="https://github.com/user-attachments/assets/b4ad7238-1d81-4074-a732-317c84115a4b" />


### Serveurs utilisés

L’application essaie successivement :

1. Cloudflare.
2. Hetzner — Falkenstein.
3. Hetzner — Helsinki.

Le passage au serveur suivant intervient en cas de refus ou d’échec avant le transfert. Un téléchargement interrompu n’est pas automatiquement relancé.

## Comprendre les résultats

Le résultat représente une **estimation du débit Internet descendant**, obtenue avec une seule connexion HTTPS vers un serveur de test.

Il dépend notamment :

- du serveur distant ;
- du trafic sur le réseau ;
- de la connexion Internet ;
- des autres téléchargements en cours ;
- de l’utilisation éventuelle d’un VPN.

> **Le test utilise la connexion active du PC.**
> Si Ethernet ou un VPN est actif, le trafic peut passer par cette connexion.
> Le résultat ne mesure pas directement la vitesse radio entre le PC et le routeur.

### Unités

| Unité | Signification |
|---|---|
| Mbit/s | Mégabits par seconde |
| Mo/s | Mégaoctets par seconde |
| Mo | Volume de données reçues |

**1 Mo/s = 8 Mbit/s**, avec les unités décimales utilisées par l’application.

## Encodage et accents

Le scan demande une sortie Windows en **UTF-8**, puis la décode en UTF-8 strict. Un repli **CP850** est prévu pour les anciennes versions qui conservent un encodage OEM.

La configuration globale de Windows n’est pas modifiée.

Les accents de l’interface sont également protégés par des séquences Unicode Java, comme `\u00e9` pour `é`.

## Dépannage

### Erreur HTTP 403

Le serveur refuse la requête. L’application essaie automatiquement le serveur suivant et affiche les détails de chaque tentative.

### Erreur HTTPS

Vérifiez votre connexion et utilisez un **JDK 8 à jour**.

Ne désactivez pas la vérification des certificats.

### Aucun réseau détecté

Vérifiez que :

- la carte Wi-Fi est activée ;
- le mode avion est désactivé ;
- des réseaux sont disponibles à proximité ;
- Windows autorise l’accès aux informations Wi-Fi et, si nécessaire, à la localisation.

### Caractères incorrects

Sélectionnez **UTF-8** dans les propriétés du projet NetBeans, remplacez entièrement l’ancienne classe, puis faites **Clean and Build**.

Si un ancien Windows ignore la sortie UTF-8, l’encodage de repli peut être adapté avec l’option VM :

```text
-Dwifi.charset=Cp850
```

## Limites actuelles

- Scan Wi-Fi disponible uniquement sous Windows.
- Actualisation des réseaux dépendante des informations fournies par Windows.
- Aucune connexion automatique aux réseaux détectés.
- Aucune mesure du débit montant ou du ping.
- Aucune mesure directe de la vitesse radio Wi-Fi.
- Disponibilité des serveurs de test dépendante des services externes.
- Mesures courtes et réalisées avec une seule connexion : les résultats restent indicatifs.

## Vérifications effectuées

- Compilation avec Java 8.
- Vérification du décodage UTF-8, CP850 et UTF-8 avec marqueur BOM.
- Vérification du passage à un serveur de secours après une réponse HTTP 403 sur un serveur local simulé.
- Vérification du rejet des réponses HTML à la place d’un fichier de test.
- Contrôle visuel de l’interface.

Les performances et l’accès aux serveurs externes restent dépendants de l’environnement d’exécution.

## Évolutions envisagées

- [ ] Présentation des réseaux dans un tableau avec tri et filtrage.
- [ ] Historique des mesures.
- [ ] Export des résultats au format CSV.
- [ ] Mesure du débit montant.
- [ ] Mesure de la latence.

## Contributions

Les signalements de bugs et les propositions d’amélioration sont les bienvenus.

Pour faciliter l’analyse d’un problème, indiquez :

- votre version de Windows ;
- votre version du JDK ;
- les étapes de reproduction ;
- le message d’erreur obtenu.

## Références

- [Documentation Microsoft — netsh wlan](https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/netsh-wlan)
- [Documentation Microsoft — chcp](https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/chcp)
- [Documentation Java 8 — SwingWorker](https://docs.oracle.com/javase/8/docs/api/javax/swing/SwingWorker.html)
- [Cloudflare Speedtest](https://github.com/cloudflare/speedtest)
- [Fichiers de test Hetzner](https://docs.hetzner.com/storage/object-storage/troubleshooting/bandwidth-latency-issues/)
