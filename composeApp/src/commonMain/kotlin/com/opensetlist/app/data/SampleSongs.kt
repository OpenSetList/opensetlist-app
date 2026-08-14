package com.opensetlist.app.data

import com.opensetlist.app.model.Setlist
import com.opensetlist.app.model.Song

/**
 * Músicas e setlists de exemplo exibidos na primeira execução.
 *
 * @author ruanitto
 */
object SampleSongs {
    val songs = listOf(
        Song(
            id = 1L,
            title = "Garota de Ipanema",
            artist = "Tom Jobim",
            key = "F",
            duration = "3:15",
            body = """
{title: Garota de Ipanema}
{artist: Tom Jobim}
{key: F}

[F7M]Olha que coisa mais [G7]linda
Mais [Gm7]cheia de graça
[C7]É ela menina que [F7M]vem e que [Am7]passa
Num doce [Dm7]balanço, a caminho do [G7]mar
[F7M]Moça do corpo dou[G7]rado
Do [Gm7]sol de Ipanema
[C7]O seu balançado é [F7M]mais que um poema
É a coisa mais [G7]linda que eu [Gm7]já vi [C7]passar
[F#7M]Ah, por que estou tão [B7]sozinho?
[F#m7]Ah, por que tudo é tão [D7]triste?
[Gm7]Ah, a beleza que [Eb7]existe
[Am7]A beleza que não é só [D7]minha
[Gm7]Que também passa [C7]sozinha
[F7M]Ah, se ela soubesse [G7]que quando ela passa
O mundo sorrindo se [Gm7]enche de graça
[C7]E fica mais lindo por [F7M]causa do amor
[G7]Por causa do amor
[Gm7]Por causa do amor
[C7]Por causa do [F7M]amor
            """.trimIndent()
        ),
        Song(
            id = 2L,
            title = "Águas de Março",
            artist = "Tom Jobim",
            key = "Am",
            duration = "4:05",
            body = """
{title: Águas de Março}
{artist: Tom Jobim}
{key: Am}

[Am]É pau, é pedra
É o fim do caminho
[G7]É um resto de toco
É um pouco sozinho
[C7M]É um caco de vidro
É a vida, é o sol
[F7]É a noite, é a morte
É o laço, é o anzol
[Am]É peroba do campo
É o nó da madeira
[G7]Caingá, candeia
É o matita-pereira
[C7M]É madeira de vento
Tombo da ribanceira
[F7]É o mistério profundo
É o queira ou não queira
[E7]É o vento vetando
É o fim da ladeira
[Am]É a viga, é o vão
Festa da cumeeira
            """.trimIndent()
        ),
        Song(
            id = 3L,
            title = "O Leãozinho",
            artist = "Caetano Veloso",
            key = "C",
            duration = "3:10",
            body = """
{title: O Leãozinho}
{artist: Caetano Veloso}
{key: C}

[C]Gosto de te ver leãozinho
[G7]Caminhando sob o [Am]sol
[Em]Gosto muito de [F]você leãozinho
[Bb]Para desentriste[C]cer leãozinho
[G7]O meu coração tão [Am]só
[Em]Basta encontrar [F]você no caminho
[Bb]Um filhote de [C]leão, raio da manhã
[F]Arrastando o meu olhar [G7]como um imã
[Am]O meu coração é o [F]sol, pai de toda [C]cor
[G7]Quando ele lhe doura a pele ao léu
[C]Gosto de te ver ao sol leãozinho
[G7]De te ver entrar no [Am]mar
[Em]Tua pele, tua [F]luz, tua [Bb]juba
[C]Gosto de ficar ao sol leãozinho
[G7]De molhar minha [Am]juba
[Em]De estar perto de [F]você e [Bb]entrar numa
            """.trimIndent()
        ),
        Song(
            id = 4L,
            title = "Chega de Saudade",
            artist = "Tom Jobim",
            key = "Dm",
            duration = "2:55",
            body = """
{title: Chega de Saudade}
{artist: Tom Jobim}
{key: Dm}

[Gm7]Vai minha tris[A7]teza e diz a [Dm]ela
Que sem [Cm6]ela não [Bm7b5]pode [Bb6]ser
Diz-lhe [A7]numa prece que [Dm7]ela re[Eb7]gresse
Porque eu [Dm7]não posso [E7]mais so[Am7]frer
[Bb6]Chega de saudade, a [A7]realidade é que sem [Dm]ela não há [Cm6]paz
Não há [Bm7b5]beleza, é só [Bb6]tristeza
E a me[A7]lancolia que não [Dm7]sai de mim
[Em7]Não sai de mim, [A7]não sai
[D7M]Mas se [B7]ela voltar, [E7]que coisa linda
[A7]Que coisa [D]louca
Pois há me[Dm7]nos peixinhos a [E7]nadar no [Am7]mar
Do que os [Gm6]beijinhos que eu [A7]darei na sua [Dm7]boca
Dentro dos [B7]meus braços, os [E7]abraços
Hão de [A7]ser milhões de [D]abraços
Aper[F#7]tado assim, co[Bm7]lado assim
Ca[Em7]lado assim
[Bb7]Abraços e beijinhos e [A7]carinhos sem ter fim
Que é pra aca[B7]bar com esse negócio de [Em7]você viver sem [A7]mim
Não quero [B7]mais esse negócio de [Em7]você viver sem [A7]mim
Vamos dei[B7]xar desse negócio de [Em7]você viver sem [Dm7]mim
            """.trimIndent()
        ),
        Song(
            id = 5L,
            title = "Roda Viva",
            artist = "Chico Buarque",
            key = "G",
            duration = "3:00",
            body = """
{title: Roda Viva}
{artist: Chico Buarque}
{key: G}

[G]Tem dias que a gente se sente
[Bm]Como quem partiu ou morreu
[C]A gente estancou de repente
[D]Ou foi o mundo então que cresceu
[G]A gente quer ter voz ativa
[Bm]No nosso destino mandar
[C]Mas eis que chega a roda viva
[D]E carrega o destino pra lá
[G]Roda [G7]mundo, [C]roda-gigante
[Cm]Roda-moinho, [G]rola [E7]pião
[Am]O tempo rodou num instante
[D]As voltas do meu coração
            """.trimIndent()
        ),
        Song(
            id = 6L,
            title = "Construção",
            artist = "Chico Buarque",
            key = "D",
            duration = "4:20",
            body = """
{title: Construção}
{artist: Chico Buarque}
{key: D}

[Dm]Amou daquela vez como se fosse a última
[Gm]Beijou sua mulher como se fosse a última
[Dm]E cada filho seu como se fosse o único
[Gm]E atravessou a rua com seu passo tímido
[A7]Subiu a construção como se fosse máquina
[Dm]Ergueu no patamar quatro paredes sólidas
[Gm]Tijolo com tijolo num desenho mágico
[Dm]Seus olhos embotados de cimento e lágrima
[Gm]Sentou pra descansar como se fosse sábado
[A7]Comeu feijão com arroz como se fosse um príncipe
[Dm]Bebeu e soluçou como se fosse um náufrago
[Gm]Dançou e gargalhou como se ouvisse música
[Dm]E tropeçou no céu como se fosse um bêbado
[Gm]E flutuou no ar como se fosse um pássaro
[A7]E terminou no chão como um pacote flácido
[Dm]Agonizou no meio do passeio público
[Gm]Morreu na contramão atrapalhando o tráfego
            """.trimIndent()
        )
    )

    val sampleSetlist = Setlist(
        id = 1L,
        name = "MPB Classics",
        songs = songs.take(4)
    )

    val allSetlists = listOf(
        sampleSetlist,
        Setlist(
            id = 2L,
            name = "Chico Buarque Night",
            songs = songs.filter { it.artist == "Chico Buarque" }
        )
    )
}
