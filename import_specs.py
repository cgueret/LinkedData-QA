import os
import shutil
from lxml import etree

ROOT = "/home/cgueret/Code/LATC/24-7-platform/link-specifications/"
OUT = "data-latc/"

def process_directory(dir_in, dir_out):
    specs = [f for f in os.listdir(dir_in) if os.path.isdir(dir_in + f)]
    for spec in specs:
        links_in = dir_in + spec + "/links.nt"
        links_out = dir_out + spec + ".nt"
        spec_xml = dir_in + spec + "/spec.xml"
        endpoints = dir_out + spec + "-named.txt"
        if os.path.isfile(links_in) and os.path.isfile(spec_xml):
            print links_out
            shutil.copy(links_in, links_out)
            tree = etree.parse(spec_xml)
            file = open(endpoints, 'w')
            for source in tree.getroot().findall("DataSources/DataSource"):
                uri = source.findall("Param[@name=\"endpointURI\"]")[0].get("value")
                graph = source.findall("Param[@name=\"graph\"]")
                if len(graph) > 0:
                    file.write(uri + " " + graph[0].get("value") + "\n")
                else:
                    file.write(uri + "\n");
            file.close()

if __name__ == '__main__':
    process_directory(ROOT, OUT)
