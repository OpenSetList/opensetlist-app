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
            id = "1",
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
[C7]É ela que [F7M]vem
Que [Dm7]passa com um doce [G7]balanço
A [Gm7]caminho do [C7]mar
[F7M]Moça do corpo dou[G7]rado
Do [Gm7]sol de Ipanema
[C7]O seu balançado [F7M]parece um poema
[Dm7]É a coisa mais linda [G7]que eu já vi pas[Gm7]sar
[C7]Ah, porque estou tão [F7M]sozinho
[Dm7]Ah, porque tudo é tão [G7]triste
[Gm7]Ah, a beleza que [C7]existe
A beleza que não é só [F7M]minha
Que também passa [G7]sozinha
[Gm7]Ah, se ela sou[C7]besse
Que quando ela [F7M]passa
O mundo inteirinho se [Dm7]enche de graça
[G7]E fica mais lindo
[Gm7]Por causa do [C7]amor
[F7M]Por causa do [G7]amor
[Gm7]Por causa do [C7]amor
[F6]Por causa do amor
            """.trimIndent()
        ),
        Song(
            id = "2",
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
            id = "3",
            title = "O Leãozinho",
            artist = "Caetano Veloso",
            key = "C",
            duration = "3:10",
            body = """
{title: O Leãozinho}
{artist: Caetano Veloso}
{key: C}

[C7M]Gosto de te ver no [G7]mar
[Am7]No vai e vem das [Dm7]ondas
[G7]Gosto de te ver e [C7M]ter
Que [Am7]nem posso explicar
[Dm7]Só sei que é [G7]como a [C7M]brisa
[Am7]Só sei que é [Dm7]como a [G7]maré
[Am7]Que lava a [Dm7]areia
[G7]Num vai e [C7M]vem sem parar
            """.trimIndent()
        ),
        Song(
            id = "4",
            title = "Chega de Saudade",
            artist = "Tom Jobim",
            key = "Dm",
            duration = "2:55",
            body = """
{title: Chega de Saudade}
{artist: Tom Jobim}
{key: Dm}

[Dm6]Vai, minha tristeza
[G7]E diz a ela que sem ela não pode ser
[Bb7M]Diz-lhe numa prece
[E7]Que ela regresse
[A7]Que eu não posso mais sofrer
[Dm6]Chega de saudade
[G7]A realidade é que sem ela não há paz
[Bb7M]Não há beleza
[E7]É só tristeza
[A7]E a melancolia que não sai de mim
[Dm6]Eu não sei viver sem ela
[G7]Não consigo ser feliz sem ela
[Bb7M]Nem a luz da lua
[E7]Me ilumina sem ela
[A7]Sem ela tudo é tão sem graça
[Dm6]Vem, me faz a graça
[G7]De voltar pra mim
            """.trimIndent()
        ),
        Song(
            id = "5",
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
            id = "6",
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
        id = "sl1",
        name = "MPB Classics",
        songs = songs.take(4)
    )

    val allSetlists = listOf(
        sampleSetlist,
        Setlist(
            id = "sl2",
            name = "Chico Buarque Night",
            songs = songs.filter { it.artist == "Chico Buarque" }
        )
    )
}
