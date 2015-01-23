import Data.List
import Data.List.Split
import System.Environment
import Debug.Trace

import Development.Shake
import Development.Shake.Command
import Development.Shake.FilePath
import Development.Shake.Util

mtrace :: Monad m => String -> m ()
mtrace = flip trace (return ())

clean = withArgs ["clean"] main

main :: IO ()
main = shakeArgs shakeOptions{shakeFiles="_build/"} $ do
    want ["_build/document.pdf"]

    phony "clean" $ do
        putNormal "Cleaning files in _build"
        removeFilesAfter "_build" ["//*"]
        removeFilesAfter "src" ["document.pdf", "*.aux", "*.out", "*.log"]

    ["_build/document.aux"] &%> \out -> do
        let source = "document.tex"
        need ["src" </> source]
        cmd (Cwd "src") "pdflatex" "-interaction=batchmode" "-output-directory=../_build/" source

    ["_build/document.pdf"] &%> \out -> do
        let source = "document.tex"
        need ["src" </> source]
        need ["_build" </> "document.bbl"]
        Stderr e <- cmd (Cwd "src") "pdflatex" "-interaction=batchmode" "-output-directory=../_build/" "-kpathsea-debug=4" source
        -- let openedFiles = map (prefixRelativePath "src") $ nub $ extractOpenedFiles e
        -- need openedFiles
        return ()

    "_build/*.bib" %> \out -> do
        let f = dropDirectory1 out
        copyFile' ("src" </> f) out

    "_build/*.bbl" %> \out -> do
        let aux = out -<.> "aux"
        let name = dropDirectory1 $ dropExtension out
        need [aux]
        auxContent <- readFile' aux
        let bibnames = extractBibNames auxContent
        need $ map (\bibname -> "_build" </> bibname <.> "bib") bibnames
        cmd (Cwd "_build") "bibtex" (dropExtension name)


extractOpenedFiles :: String -> [String]
extractOpenedFiles s = openedFiles
    where ls = lines s
          opens = filter (isPrefixOf "kdebug:fopen") ls
          openedFiles = map readFopen opens

readFopen s = drop start $ take end $ s
    where start = length "kdebug:fopen("
          Just end = findIndex (==',') s
          
extractBibNames :: String -> [String]
extractBibNames s = bibs
    where ls = lines s
          bibdatas = filter (isPrefixOf "\\bibdata{") ls
          bibs = map readBibName bibdatas

readBibName s = drop start $ take end $ s
    where start = length "\\bibdata{"
          Just end = findIndex (=='}') s


prefixRelativePath :: String -> FilePath -> FilePath
prefixRelativePath prefix p | isAbsolute p = p
prefixRelativePath prefix p | otherwise    = prefix </> p