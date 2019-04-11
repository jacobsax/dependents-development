import os
import subprocess

def run_cli(cli_tool_path, short_url, source_code_dir, jar_dir, parsing_type, output_file_path, output_log_path):    

    log_file = open(output_log_path, "w+")

    # process = subprocess.Popen("java -jar {} -i {} -j {} -s {} -l cypher -t {} -o {}".format(cli_tool_path, short_url, jar_dir, source_code_dir, parsing_type, output_file_path), shell=True, executable='/bin/bash', stdout=subprocess.DEVNULL, stderr=subprocess.STDOUT)
    process = subprocess.Popen("java -jar {} -i {} -j {} -s {} -l cypher -t {} -o {}".format(cli_tool_path, short_url, jar_dir, source_code_dir, parsing_type, output_file_path), shell=True, executable='/bin/bash', stdout=log_file, stderr=subprocess.STDOUT)

    process.wait()

    log_file.close()

    assert os.path.isfile(output_file_path) == True
    
def read_test_file(output_file_path):
    cypher_file = open(output_file_path, "r")
    result = [line. rstrip('\n') for line in cypher_file]
    cypher_file.close()

    return result

def remove_test_file(output_file_path):
    if (os.path.isfile(output_file_path) == True):
        os.remove(output_file_path)
    
    assert os.path.isfile(output_file_path) == False