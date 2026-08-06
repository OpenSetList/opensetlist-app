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

[Gm9][Dm7][Dm7/C][Bdim][Bbm6][Dm7][A7+5]
[Dm7]Vai mi[Dm7/C]nha tri[Bdim]steza e diz a [Bbm6]ela [A7/+5]que sem [Dm7]ela não pode ser
Diz-lhe [Bm7/-5]nu-[F6]ma [Am7]prece que ela re[Bb6]gresse, porque eu não [A7]posso mais so[A7/+5]frer
[Dm7]Chega de[Dm7/C] sau[Bdim]dade, a reali[Bbm6]dade é [A7/+5]que sem [Am6]ela não há [Adim]paz
Não há be[Gm7]leza, é [A7]só tris[Dm7]teza e a mel[Dm7/C]ancolia que não [Bdim]sai de mim, [Bbm6]Não sai de mim, não [Dm7]sai[Em11] [Em7] [A7/6] [A7]
[Dmaj7/F#]Mas[F#dim] se ela vol[E7/9]tar, se ela voltar, que coisa [Em7]linda,[A7/-9] que coisa [E13/-9]lou-[D7/F#]ca
Pois há menos pei[E13/-9]xinhos a na[Em7]dar no mar, Do que os bei[E7/9]jinhos que eu darei na sua [Gm6]bo-[A7/-9]ca
[Dmaj7/F#]Dentro   [D6/F#]dos meus braços os abraços [F#7]hão de [Bm7]ser mi[Bbm7]lhões de a[Am7]braços
[Adim]Aper- [Gmaj7]tado assim, co[Gm6]lado assim, ca[F#m7]lado assim, A[F#dim]braços [B7]e beijinhos e ca[Em7]rinhos sem ter [F#7]fim
Que é prá aca[B7]bar com esse negócio de vi[Em7]ver longe de [Dmaj7]mim[D6]
Não quero [F#dim]mais esse negócio de vo[Em7]cê viver a[Dmaj7]ssim[D6]
Vamos dei[F#dim]xar desse negócio de vo[Em7]cê viver sem [Dmaj7]mim[D6]
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
