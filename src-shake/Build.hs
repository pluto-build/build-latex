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

build = "bin-latex"
source = "src-latex"

main :: IO ()
main = shakeArgs shakeOptions{shakeFiles=build++"/"} $ do
    want [build</>"document.pdf"]

    phony "clean" $ do
        putNormal $ "Cleaning files in " ++ build
        removeFilesAfter build ["//*"]
        removeFilesAfter source ["document.pdf", "*.aux", "*.out", "*.log"]

    [build</>"document.aux"] &%> \out -> do
        let sourceFile = "document.tex"
        need [source </> sourceFile]
        cmd (Cwd source) "pdflatex" "-interaction=batchmode" ("-output-directory=.."</>build++"/") sourceFile

    [build</>"document.pdf"] &%> \out -> do
        let sourceFile = "document.tex"
        need [source </> sourceFile]
        need [build </> "document.bbl"]
        Stderr e <- cmd (Cwd source) "pdflatex" "-interaction=batchmode" ("-output-directory=.."</>build++"/") "-kpathsea-debug=4" sourceFile
        -- let openedFiles = map (prefixRelativePath source) $ nub $ extractOpenedFiles e
        -- need openedFiles
        return ()

    build</>"*.bib" %> \out -> do
        let f = dropDirectory1 out
        copyFile' (source </> f) out

    build</>"*.bbl" %> \out -> do
        let aux = out -<.> "aux"
        let name = dropDirectory1 $ dropExtension out
        need [aux]
        auxContent <- readFile' aux
        let bibnames = extractBibNames auxContent
        need $ map (\bibname -> build </> bibname <.> "bib") bibnames
        cmd (Cwd build) "bibtex" (dropExtension name)


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