package chaos.s18.a1o1;

import chaos.s18.a1o1.meta.C18CodedMeta;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.FieldWithMetaString;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * The meta-shape dimension at the switch subject&#39;s feeder (charter 2.2b).
 * @version 1.0.0
 */
@RosettaDataType(value="C18Coded", builder=C18Coded.C18CodedBuilderImpl.class, version="1.0.0")
@RuneDataType(value="C18Coded", model="chaos", builder=C18Coded.C18CodedBuilderImpl.class, version="1.0.0")
public interface C18Coded extends RosettaModelObject {

	C18CodedMeta metaData = new C18CodedMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaString getKind();

	/*********************** Build Methods  ***********************/
	C18Coded build();
	
	C18Coded.C18CodedBuilder toBuilder();
	
	static C18Coded.C18CodedBuilder builder() {
		return new C18Coded.C18CodedBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends C18Coded> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends C18Coded> getType() {
		return C18Coded.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("kind"), processor, FieldWithMetaString.class, getKind());
	}
	

	/*********************** Builder Interface  ***********************/
	interface C18CodedBuilder extends C18Coded, RosettaModelObjectBuilder {
		FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateKind();
		@Override
		FieldWithMetaString.FieldWithMetaStringBuilder getKind();
		C18Coded.C18CodedBuilder setKind(FieldWithMetaString kind);
		C18Coded.C18CodedBuilder setKindValue(String kind);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("kind"), processor, FieldWithMetaString.FieldWithMetaStringBuilder.class, getKind());
		}
		

		C18Coded.C18CodedBuilder prune();
	}

	/*********************** Immutable Implementation of C18Coded  ***********************/
	class C18CodedImpl implements C18Coded {
		private final FieldWithMetaString kind;
		
		protected C18CodedImpl(C18Coded.C18CodedBuilder builder) {
			this.kind = ofNullable(builder.getKind()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public FieldWithMetaString getKind() {
			return kind;
		}
		
		@Override
		public C18Coded build() {
			return this;
		}
		
		@Override
		public C18Coded.C18CodedBuilder toBuilder() {
			C18Coded.C18CodedBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(C18Coded.C18CodedBuilder builder) {
			ofNullable(getKind()).ifPresent(builder::setKind);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18Coded _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C18Coded {" +
				"kind=" + this.kind +
			'}';
		}
	}

	/*********************** Builder Implementation of C18Coded  ***********************/
	class C18CodedBuilderImpl implements C18Coded.C18CodedBuilder {
	
		protected FieldWithMetaString.FieldWithMetaStringBuilder kind;
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public FieldWithMetaString.FieldWithMetaStringBuilder getKind() {
			return kind;
		}
		
		@Override
		public FieldWithMetaString.FieldWithMetaStringBuilder getOrCreateKind() {
			FieldWithMetaString.FieldWithMetaStringBuilder result;
			if (kind!=null) {
				result = kind;
			}
			else {
				result = kind = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@RosettaAttribute("kind")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("kind")
		@Override
		public C18Coded.C18CodedBuilder setKind(FieldWithMetaString _kind) {
			this.kind = _kind == null ? null : _kind.toBuilder();
			return this;
		}
		
		@Override
		public C18Coded.C18CodedBuilder setKindValue(String _kind) {
			this.getOrCreateKind().setValue(_kind);
			return this;
		}
		
		@Override
		public C18Coded build() {
			return new C18Coded.C18CodedImpl(this);
		}
		
		@Override
		public C18Coded.C18CodedBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18Coded.C18CodedBuilder prune() {
			if (kind!=null && !kind.prune().hasData()) kind = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getKind()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public C18Coded.C18CodedBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			C18Coded.C18CodedBuilder o = (C18Coded.C18CodedBuilder) other;
			
			merger.mergeRosetta(getKind(), o.getKind(), this::setKind);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			C18Coded _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "C18CodedBuilder {" +
				"kind=" + this.kind +
			'}';
		}
	}
}
