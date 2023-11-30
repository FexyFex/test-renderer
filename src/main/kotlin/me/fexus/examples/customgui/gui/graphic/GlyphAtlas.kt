package me.fexus.examples.customgui.gui.graphic

import me.fexus.math.vec.IVec2
import me.fexus.texture.TextureLoader
import kotlin.math.floor


class GlyphAtlas {
    companion object {
        const val ROWS: Int = 9
        const val COLUMNS: Int = 5
    }

    val texture = TextureLoader("textures/customgui/glyphs.png")
    val glyphWidth = texture.width / COLUMNS
    val glyphHeight = texture.height / ROWS

    val legalChars = arrayOf(
        '0','1','2','3','4','5','6','7','8','9',
        'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
        'a','b','c','d','e','f','g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v','w','x','y','z',
        '.','_',' ',':','_','-','!','?'
    )


    fun getGlyphBounds(char: Char): GlyphBounds {
        // check if letter
        val indexL = char.code - 97
        if (indexL in 0..25) {
            val fColumns = COLUMNS.toFloat()
            val x = indexL % COLUMNS
            val y = floor(indexL / fColumns).toInt()
            val xTexCoord = (x / fColumns * texture.width).toInt()
            val yTexCoord = (y / ROWS.toFloat() * texture.height).toInt()
            val min = IVec2(xTexCoord, yTexCoord)
            val max = IVec2(xTexCoord + glyphWidth, yTexCoord + glyphHeight)
            return GlyphBounds(min, max)
            //return memAlloc(1).copyTextureRegion(texture.width, xTexCoord, yTexCoord, glyphWidth, glyphHeight)
        }

        // check if number
        val indexN = char.code - 48
        if (indexN in 0..9) {
            val fColumns = COLUMNS.toFloat()
            val x = (indexN % COLUMNS)
            val y = floor(indexN / fColumns).toInt() + 6
            val xTexCoord = (x / fColumns * texture.width).toInt()
            val yTexCoord = (y / ROWS.toFloat() * texture.height).toInt()
            val min = IVec2(xTexCoord, yTexCoord)
            val max = IVec2(xTexCoord + glyphWidth, yTexCoord + glyphHeight)
            return GlyphBounds(min, max)
            //return texture.pixels.copyTextureRegion(texture.width, xTexCoord, yTexCoord, glyphWidth, glyphHeight)
        }

        // check if other
        val pos: IVec2 = when (char) {
            ('_') -> IVec2(1, 5)
            ('.') -> IVec2(2, 5)
            ('-') -> IVec2(0, 8)
            ('!') -> IVec2(1, 8)
            ('?') -> IVec2(2, 8)
            (':') -> IVec2(3, 5)
            else -> IVec2(4, 5) // if no match was found, print empty space
        }

        val min = IVec2(pos.x * glyphWidth, pos.y * glyphHeight)
        val max = IVec2(pos.x * glyphWidth + glyphWidth, pos.y * glyphHeight + glyphHeight)
        return GlyphBounds(min, max)
        //return texture.pixels.copyTextureRegion(texture.width, pos.x * glyphWidth, pos.y * glyphHeight, glyphWidth, glyphHeight)
    }


    data class GlyphBounds(val min: IVec2, val max: IVec2)
}