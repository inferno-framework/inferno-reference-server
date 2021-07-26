require 'httparty'
require 'pry'

FHIR_SERVER = 'http://localhost:8080/reference-server/r4'
TOKEN = 'SAMPLE_TOKEN'

def upload_us_core_resources
  file_path = File.join(__dir__, 'us-core-r4-resources', '*.json')
  filenames = Dir.glob(file_path)
                .select { |filename| filename.end_with? '.json' }
  puts "#{filenames.length} resources to upload"
  old_retry_count = filenames.length
  loop do
    filenames_to_retry = []
    filenames.each do |filename|
      resource = JSON.parse(File.read(filename), symbolize_names: true)
      puts "Uploading #{filename} (#{resource[:resourceType]})"
      if resource[:resourceType] == 'Bundle' && resource[:type] == 'transaction'

        patient_identifier = patient_identifier_in_transaction(resource)
        record_exists = record_exists_on_server?(patient_identifier)

        if record_exists
          puts "Patient with identifier #{patient_identifier} already exists, skipping."
        else
          response = execute_transaction(resource)
          if !response.success?
            puts "Error uploading #{filename}: #{response.body}"
            filenames_to_retry << filename
          end
        end
      else
        response = upload_resource(resource)
        if !response.success?
          puts "Error uploading #{filename}: #{response.body}"
          filenames_to_retry << filename
        end
      end

    end
    break if filenames_to_retry.empty?
    retry_count = filenames_to_retry.length
    if retry_count == old_retry_count
      puts "Unable to upload #{retry_count} resources:"
      puts filenames.join("\n")
      break
    end
    puts "#{retry_count} resources to retry"
    filenames = filenames_to_retry
    old_retry_count = retry_count
  end
end


def combine_files_into_single_transaction_json
    entries = []
    exempt_resource_types = ["CapabilityStatement", "CodeSystem", "ConceptMap", "ImplementationGuide", "OperationDefinition", "SearchParameter", "StructureDefinition", "ValueSet"]

    file_path = File.join(__dir__, 'us-core-r4-resources', '*.json')
    filenames = Dir.glob(file_path)
                .select { |filename| filename.end_with? '.json' }
      
    resources_to_upload_separately = []
    filenames.each do |filename|
      resource = JSON.parse(File.read(filename), symbolize_names: true)
      puts "Adding #{filename} to transaction"

      if resource[:type] == 'transaction'
        entries = entries.concat(resource[:entry])
      elsif (exempt_resource_types.include? resource[:resourceType])
        resources_to_upload_separately.push(resource)
      else
        #wrap resource in entry
        fullUrl = "urn:uuid:" + resource[:id]
        resourceType = resource[:resourceType]

        entry = {
          "fullUrl": fullUrl,
          "resource": resource,
          "request": {
            "method": "POST",
            "url": resourceType
          }

        }
        entries.push(entry)
      end
    end

    transactionJson = {
        "type": "transaction",
        "entry": entries,
        "resourceType": "Bundle",        
    }

    response = execute_transaction(transactionJson)

    if !response.success?
      puts "Error uploading transaction because: #{response.body}"
    end

    resources_to_upload_separately.each do |resource|
      response = upload_resource(resource)
      if !response.success?
          puts "Error uploading #{resource}: #{response.body}"
      end
    end
end


def upload_resource(resource)
  resource_type = resource[:resourceType]
  id = resource[:id]
  HTTParty.put(
    "#{FHIR_SERVER}/#{resource_type}/#{id}",
    body: resource.to_json,
    headers: {
     'Content-Type': 'application/json',
     'Authorization': "Bearer #{TOKEN}"
    }
  )
end

def patient_identifier_in_transaction(transaction)

  patient_record = transaction[:entry]&.find {|r| r[:resource][:resourceType] == 'Patient'}
  identifier = patient_record[:resource][:identifier].first
  "#{identifier[:system]}|#{identifier[:value]}"
end

def record_exists_on_server?(patient_identifier)

  response = HTTParty.get(
    "#{FHIR_SERVER}/Patient",
    query: { identifier: patient_identifier },
    headers: {
      'Content-Type': 'application/json',
      'Authorization': "Bearer #{TOKEN}"
     }
  )
  JSON.parse(response.body)['entry']&.any?

end

def execute_transaction(transaction)

  HTTParty.post(
    FHIR_SERVER,
    body: transaction.to_json,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': "Bearer #{TOKEN}"
     },
     timeout: 600
  )
end

combine_files_into_single_transaction_json
